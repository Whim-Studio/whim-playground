package com.whim.bc3k.sim.combat;

import com.whim.bc3k.api.Enums;
import com.whim.bc3k.sim.ship.ShipSystems;

/**
 * Deterministic space combat for Xtreme Carnage. The player capital ship is
 * supplied by the engine (damage lands on the real ship); the enemy is owned here.
 *
 * Two layers now resolve together:
 *  - Capital exchange: the player fires volleys on command; the enemy auto-fires
 *    on a fixed cadence. Volley damage scales with WEAPONS power × integrity, and
 *    hits degrade a targeted subsystem (so ENGINEERING repair matters in a fight).
 *  - Fighter dogfight: launched player fighters and the enemy wing attrite each
 *    other and strafe the opposing capital ship.
 *
 * No randomness — outcomes are a pure function of allocations, fighters committed,
 * and elapsed time — so combat stays unit-testable.
 */
public final class CombatSim {

    private static final double VOLLEY_COEFF     = 12.0;  // capital damage per weapon power unit
    private static final double ENEMY_CADENCE     = 1.5;   // seconds between enemy volleys
    private static final double FIGHTER_ATTRITION = 0.15;  // fighters lost per opposing fighter per second
    private static final double FIGHTER_STRAFE    = 6.0;   // capital damage per surviving fighter per second

    private final ShipSystems player;
    private final ShipSystems enemy;
    private final String enemyName;

    private double enemyCooldown = ENEMY_CADENCE;
    private double playerFighters = 0;
    private double enemyFighters = 3;
    private boolean over;
    private boolean playerWon;

    public CombatSim(ShipSystems player, String enemyName) {
        this.player = player;
        this.enemyName = enemyName;
        this.enemy = new ShipSystems();
        // Enemy shields do not regenerate (the enemy ship is never ticked here), so
        // sustained fire wins — no need to hack its power allocation.
    }

    public String enemyName() { return enemyName; }
    public ShipSystems enemy() { return enemy; }
    public boolean over() { return over; }
    public boolean playerWon() { return playerWon; }
    public int playerFighters() { return (int) Math.round(playerFighters); }
    public int enemyFighters() { return (int) Math.round(enemyFighters); }

    /** Commit launched fighters to the dogfight. */
    public void addPlayerFighters(int n) { playerFighters = Math.max(0, playerFighters + n); }
    public void removePlayerFighters(int n) { playerFighters = Math.max(0, playerFighters - n); }

    private static double volleyDamage(ShipSystems s) {
        ShipSystems.Subsystem w = s.system(Enums.PowerSystem.WEAPONS);
        return VOLLEY_COEFF * w.power() * w.effectiveness();
    }

    /** Player fires all powered weapons at the enemy; hits knock out enemy weapons over time. */
    public double playerVolley() {
        if (over) return 0;
        double dmg = volleyDamage(player);
        enemy.applyDamage(dmg, Enums.PowerSystem.WEAPONS);
        checkOver();
        return dmg;
    }

    /** Advance the dogfight and the enemy's capital cadence fire. */
    public void tick(double dt) {
        if (over) return;

        if (playerFighters > 0 || enemyFighters > 0) {
            double pLoss = enemyFighters * FIGHTER_ATTRITION * dt;
            double eLoss = playerFighters * FIGHTER_ATTRITION * dt;
            playerFighters = Math.max(0, playerFighters - pLoss);
            enemyFighters = Math.max(0, enemyFighters - eLoss);
            if (playerFighters > 0) enemy.applyDamage(playerFighters * FIGHTER_STRAFE * dt, null);
            if (enemyFighters > 0) player.applyDamage(enemyFighters * FIGHTER_STRAFE * dt, Enums.PowerSystem.SENSORS);
        }

        enemyCooldown -= dt;
        if (enemyCooldown <= 0) {
            enemyCooldown += ENEMY_CADENCE;
            player.applyDamage(volleyDamage(enemy), Enums.PowerSystem.SHIELDS);
        }
        checkOver();
    }

    private void checkOver() {
        if (enemy.destroyed()) { over = true; playerWon = true; }
        else if (player.destroyed()) { over = true; playerWon = false; }
    }
}
