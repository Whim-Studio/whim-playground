package com.whim.starcommand.engine;

import com.whim.starcommand.model.Character;
import com.whim.starcommand.model.Ship;
import com.whim.starcommand.model.Weapon;

import java.util.ArrayList;
import java.util.List;

/**
 * Turn-based ship-to-ship combat resolution, decoupled from Swing so it can be
 * unit-tested headlessly. Each call resolves one player action followed by one
 * enemy action, appending human-readable log lines.
 */
public class CombatEngine {

    public enum Action { FIRE_BEAM, FIRE_MISSILE, RAISE_SHIELDS, DISABLE, FLEE }

    /** Outcome of the whole battle once it terminates. */
    public enum Result { ONGOING, ENEMY_DESTROYED, ENEMY_DISABLED, PLAYER_DESTROYED, PLAYER_FLED }

    private final Rng rng;
    private final Ship player;
    private final Ship enemy;
    private final Character captain; // best pilot; boosts accuracy
    public Result result = Result.ONGOING;

    public CombatEngine(Rng rng, Ship player, Ship enemy, Character captain) {
        this.rng = rng;
        this.player = player;
        this.enemy = enemy;
        this.captain = captain;
    }

    /** Resolve one full round; returns log lines produced this round. */
    public List<String> round(Action action) {
        List<String> log = new ArrayList<String>();
        if (result != Result.ONGOING) return log;

        // --- player phase ---
        switch (action) {
            case FIRE_BEAM:
                fire(player, enemy, Weapon.Type.BEAM, false, log, "You");
                break;
            case FIRE_MISSILE:
                fire(player, enemy, Weapon.Type.MISSILE, false, log, "You");
                break;
            case DISABLE:
                // aim for engines: reduced damage but far likelier to disable, not destroy
                fire(player, enemy, null, true, log, "You");
                break;
            case RAISE_SHIELDS:
                int regen = Math.max(4, player.maxShield / 4);
                player.shield = Math.min(player.maxShield, player.shield + regen);
                log.add("You divert power to shields (+" + regen + ").");
                break;
            case FLEE:
                if (rng.chance(40 + player.engines * 6 - enemy.engines * 4)) {
                    result = Result.PLAYER_FLED;
                    log.add("You break off and jump clear.");
                    return log;
                }
                log.add("Escape vector blocked — the enemy holds you in.");
                break;
        }

        if (checkEnemyDown(log)) return log;

        // --- enemy phase ---
        enemyTurn(log);
        checkPlayerDown(log);
        return log;
    }

    private void fire(Ship from, Ship target, Weapon.Type onlyType, boolean disableAim,
                      List<String> log, String who) {
        boolean fired = false;
        for (Weapon w : from.weapons) {
            if (onlyType != null && w.type != onlyType) continue;
            fired = true;
            int acc = w.accuracy + captainBonus();
            if (disableAim) acc -= 15; // called shot at engines is harder
            if (rng.chance(acc)) {
                int dmg = rng.range(w.minDamage, w.maxDamage);
                if (disableAim) dmg = Math.max(1, dmg / 2);
                boolean nowDisabled = target.takeDamage(dmg);
                log.add(who + " hit with " + w.name + " for " + dmg + ".");
                if (disableAim && !nowDisabled && target.hull <= target.maxHull / 4) {
                    // finishing a disable attempt cripples engines
                    target.hull = 0;
                    target.disabled = true;
                    log.add(who + " cripple the enemy's engines — she's dead in space!");
                }
            } else {
                log.add(who + " missed with " + w.name + ".");
            }
        }
        if (!fired) log.add(who + " have no weapon of that type fitted.");
    }

    private void enemyTurn(List<String> log) {
        if (enemy.disabled) return;
        if (enemy.shield < enemy.maxShield / 3 && rng.chance(30)) {
            int regen = Math.max(3, enemy.maxShield / 4);
            enemy.shield = Math.min(enemy.maxShield, enemy.shield + regen);
            log.add("Enemy reinforces shields (+" + regen + ").");
            return;
        }
        fire(enemy, player, null, false, log, "Enemy");
    }

    private int captainBonus() {
        if (captain == null) return 0;
        return captain.accuracy / 4; // seasoned gunner improves hit chance
    }

    private boolean checkEnemyDown(List<String> log) {
        if (enemy.disabled) {
            result = Result.ENEMY_DISABLED;
            log.add("Enemy ship disabled — prepare a boarding party.");
            return true;
        }
        if (enemy.hull <= 0) {
            result = Result.ENEMY_DESTROYED;
            log.add("Enemy ship destroyed.");
            return true;
        }
        return false;
    }

    private void checkPlayerDown(List<String> log) {
        if (player.hull <= 0) {
            result = Result.PLAYER_DESTROYED;
            log.add("Your ship is torn apart. All hands lost.");
        }
    }
}
