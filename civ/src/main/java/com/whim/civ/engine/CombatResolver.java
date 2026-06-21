package com.whim.civ.engine;

import com.whim.civ.domain.Terrain;
import com.whim.civ.domain.Unit;

import java.util.Random;

/**
 * Civilization-1-style HP attrition duel.
 *
 * <p>Effective strengths:
 * <pre>
 *   attacker = type.getAttack()  * (veteran ? 1.5 : 1.0)
 *   defender = type.getDefense() * (veteran ? 1.5 : 1.0)
 *            * (1 + terrainDefenseBonusPct/100)
 *            * (fortified  ? 1.5 : 1.0)
 *            * (cityWalls  ? 3.0 : 1.0)
 * </pre>
 * Each round the attacker wins with probability {@code attackS / (attackS + defenseS)};
 * the loser of the round loses 1 HP. Rounds repeat until one unit reaches 0 HP. HP is
 * mutated in place; {@link #resolveCombat} returns {@code true} iff the attacker survives.
 */
public final class CombatResolver {

    private final Random rng;

    public CombatResolver(Random rng) {
        this.rng = rng;
    }

    public double attackStrength(Unit attacker) {
        double a = attacker.getType().getAttack();
        if (attacker.isVeteran()) {
            a *= 1.5;
        }
        return a;
    }

    public double defenseStrength(Unit defender, Terrain terrain,
                                  boolean fortified, boolean cityWalls) {
        double d = defender.getType().getDefense();
        if (defender.isVeteran()) {
            d *= 1.5;
        }
        d *= (1.0 + terrain.getDefenseBonusPct() / 100.0);
        if (fortified) {
            d *= 1.5;
        }
        if (cityWalls) {
            d *= 3.0;
        }
        return d;
    }

    /**
     * Resolve a full duel. Mutates both units' hit points. Returns true iff the attacker
     * is the survivor (defender reached 0 HP first).
     */
    public boolean resolveCombat(Unit attacker, Unit defender, Terrain terrain,
                                 boolean fortified, boolean cityWalls) {
        double attackS = attackStrength(attacker);
        double defenseS = defenseStrength(defender, terrain, fortified, cityWalls);
        double total = attackS + defenseS;

        // Degenerate guard: if both strengths are zero, treat as an even coin flip.
        double p = (total <= 0.0) ? 0.5 : attackS / total;

        while (attacker.isAlive() && defender.isAlive()) {
            if (rng.nextDouble() < p) {
                defender.setHitPoints(defender.getHitPoints() - 1);
            } else {
                attacker.setHitPoints(attacker.getHitPoints() - 1);
            }
        }
        return attacker.isAlive();
    }
}
