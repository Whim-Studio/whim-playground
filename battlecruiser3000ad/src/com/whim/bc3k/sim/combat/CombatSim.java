package com.whim.bc3k.sim.combat;

import com.whim.bc3k.api.Enums;
import com.whim.bc3k.sim.ship.ShipSystems;

/**
 * Deterministic ship-to-ship combat used by Xtreme Carnage. The player ship is
 * supplied by the engine (so damage lands on the real ship); the enemy is owned
 * here. The player fires volleys on command; the enemy auto-fires on a fixed
 * cadence. Volley damage scales with the firing ship's WEAPONS power × integrity,
 * so the PWR console meaningfully affects the fight.
 *
 * No randomness — outcomes are a pure function of allocations and elapsed time —
 * which keeps combat unit-testable.
 */
public final class CombatSim {

    private static final double VOLLEY_COEFF   = 12.0;  // damage per weapon power unit
    private static final double ENEMY_CADENCE  = 1.5;   // seconds between enemy volleys

    private final ShipSystems player;
    private final ShipSystems enemy;
    private final String enemyName;

    private double enemyCooldown = ENEMY_CADENCE;
    private boolean over;
    private boolean playerWon;

    public CombatSim(ShipSystems player, String enemyName) {
        this.player = player;
        this.enemyName = enemyName;
        this.enemy = new ShipSystems();
        // A raider: no shield regen (drain shield power) so the fight can resolve.
        this.enemy.allocate(Enums.PowerSystem.SHIELDS, -this.enemy.system(Enums.PowerSystem.SHIELDS).power());
    }

    public String enemyName() { return enemyName; }
    public ShipSystems enemy() { return enemy; }
    public boolean over() { return over; }
    public boolean playerWon() { return playerWon; }

    private static double volleyDamage(ShipSystems s) {
        ShipSystems.Subsystem w = s.system(Enums.PowerSystem.WEAPONS);
        return VOLLEY_COEFF * w.power() * w.effectiveness();
    }

    /** Player fires all powered weapons at the enemy. Returns damage dealt. */
    public double playerVolley() {
        if (over) return 0;
        double dmg = volleyDamage(player);
        enemy.applyDamage(dmg, null);
        checkOver();
        return dmg;
    }

    /** Advance the enemy: regen (none, by design) and auto-fire on cadence. */
    public void tick(double dt) {
        if (over) return;
        enemy.tick(dt);
        enemyCooldown -= dt;
        if (enemyCooldown <= 0) {
            enemyCooldown += ENEMY_CADENCE;
            player.applyDamage(volleyDamage(enemy), null);
        }
        checkOver();
    }

    private void checkOver() {
        if (enemy.destroyed()) { over = true; playerWon = true; }
        else if (player.destroyed()) { over = true; playerWon = false; }
    }
}
