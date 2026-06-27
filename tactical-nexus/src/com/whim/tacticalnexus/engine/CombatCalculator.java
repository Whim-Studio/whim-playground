package com.whim.tacticalnexus.engine;

import com.whim.tacticalnexus.domain.Enemy;
import com.whim.tacticalnexus.domain.Player;

/**
 * Deterministic combat math (classic Tower-of-the-Sorcerer rules, player strikes
 * first). Pure: no RNG, no Swing, no state mutation.
 *
 * <pre>
 *   if playerATK <= enemyDEF      -> canFight=false (enemy invincible)
 *   playerDamage = playerATK - enemyDEF
 *   hitsToKill   = ceil(enemyHP / playerDamage)
 *   enemyDamage  = max(0, enemyATK - playerDEF)
 *   hpLost       = enemyDamage * (hitsToKill - 1)   // enemy dies on player's last strike
 *   survivable   = hpLost < player.hp()             // must strictly survive
 * </pre>
 */
public final class CombatCalculator {

    private CombatCalculator() {
    }

    public static CombatResult resolve(Player p, Enemy e) {
        int playerAtk = p.atk();
        int enemyDef = e.def();

        // Enemy invincible: a bump behaves like a wall.
        if (playerAtk <= enemyDef) {
            return new CombatResult(false, 0, 0, false);
        }

        int playerDamage = playerAtk - enemyDef; // > 0 here
        int enemyHp = e.hp();
        int hitsToKill = ceilDiv(enemyHp, playerDamage);

        int enemyDamage = Math.max(0, e.atk() - p.def());
        int hpLost = enemyDamage * (hitsToKill - 1);

        boolean survivable = hpLost < p.hp();
        return new CombatResult(true, hitsToKill, hpLost, survivable);
    }

    /** Ceiling integer division for non-negative numerator and positive denominator. */
    private static int ceilDiv(int numerator, int denominator) {
        return (numerator + denominator - 1) / denominator;
    }
}
