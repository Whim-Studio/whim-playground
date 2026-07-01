package com.whim.warroom.engine;

import com.whim.warroom.domain.MapState;
import com.whim.warroom.domain.Stance;
import com.whim.warroom.domain.Unit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Deterministic per-tick engagement damage.
 *
 * Each alive, un-routed unit attacks its nearest enemy inside weapon range once
 * per tick (focus fire). Damage follows the contract formula:
 *
 *   dmg = attack * stanceAtkMul * (1 - defenderCoverDef) / TICKS_PER_SECOND
 *
 * where {@code defenderCoverDef} folds the defender's defense points, the
 * terrain cover bonus at the defender's position, and the defender's stance into
 * a single damage-reduction fraction clamped to [0, 0.9].
 *
 * The tiny RNG variance (via the shared seed) is applied deterministically so a
 * replay from tick 0 reproduces identical results.
 */
final class CombatResolver {

    /** Damage per-tick taken by each unit this tick (id -> total), used by morale. */
    final Map<Integer, Double> damageThisTick = new HashMap<Integer, Double>();

    static double stanceAtkMul(Stance s) {
        switch (s) {
            case OFFENSIVE: return 1.4;
            case RETREAT:   return 0.4;
            case DEFENSIVE:
            default:        return 1.0;
        }
    }

    static double stanceDefBonus(Stance s) {
        switch (s) {
            case DEFENSIVE: return 0.20;
            case RETREAT:   return -0.10;
            case OFFENSIVE:
            default:        return -0.05;
        }
    }

    /** Convert abstract defense points to a base damage-reduction fraction. */
    static double defenseFraction(double defensePoints) {
        // Diminishing returns: 50 pts ~ 0.5, asymptotes below 1.
        return defensePoints / (defensePoints + 50.0);
    }

    /**
     * Total damage-reduction fraction the defender enjoys, clamped to [0, 0.9].
     */
    static double defenderCoverDef(Unit defender, MapState map) {
        double frac = defenseFraction(defender.getType().getDefense());
        double cover = map.coverBonusAtWorld(defender.getPos().x, defender.getPos().y);
        double stance = stanceDefBonus(defender.getStance());
        double total = frac + cover + stance;
        if (total < 0) total = 0;
        if (total > 0.9) total = 0.9;
        return total;
    }

    /** Resolve all engagements for this tick, mutating unit health. */
    void resolve(List<Unit> units, MapState map, int tick, Random rng) {
        damageThisTick.clear();
        // Snapshot targets first so ordering within a tick is stable/deterministic.
        for (int i = 0; i < units.size(); i++) {
            Unit a = units.get(i);
            if (!a.isAlive() || a.isRouted()) {
                continue;
            }
            Unit target = AiController.nearestEnemy(a, units);
            if (target == null) {
                continue;
            }
            double d = a.getPos().dist(target.getPos());
            if (d > a.getType().getRange()) {
                continue;
            }
            double atkMul = stanceAtkMul(a.getStance());
            double coverDef = defenderCoverDef(target, map);
            double base = a.getType().getAttack() * atkMul * (1.0 - coverDef)
                    / SimEngineImpl.TICKS_PER_SECOND;
            // Small deterministic variance (+/-5%) sourced from the shared RNG.
            double variance = 0.95 + rng.nextDouble() * 0.10;
            double dmg = base * variance;
            if (dmg <= 0) {
                continue;
            }
            target.setHealth(target.getHealth() - dmg);
            Double prev = damageThisTick.get(target.getId());
            damageThisTick.put(target.getId(), (prev == null ? 0.0 : prev) + dmg);
        }
    }
}
