package com.whim.powermonger.engine;

import com.whim.powermonger.api.Enums.Posture;

/**
 * Pure real-time skirmish math. No domain dependency — operates on primitives so it
 * can be unit-tested and reused. {@link CombatResolver} pulls values off the domain
 * blocs and feeds them here each tick two opposing blocs are co-located.
 *
 * Model: each side's "effective power" combines raw strength, posture aggression,
 * an elevation (high-ground) advantage, and a food/morale factor. The stronger side
 * inflicts casualties on the weaker proportional to the power ratio, per tick, so a
 * skirmish plays out over several ticks rather than instantly.
 */
public final class CombatMath {
    private CombatMath() {}

    /** Baseline fraction of the loser's strength shed per combat tick. */
    public static final double BASE_CASUALTY_RATE = 0.04;

    /** Per elevation-band advantage, multiply effective power by this. */
    public static final double ELEVATION_BONUS_PER_BAND = 0.12;

    /**
     * Effective combat power of a bloc. Strength scaled by posture aggression, high
     * ground, and morale (food per man). Never negative.
     */
    public static double effectivePower(int strength, Posture posture, int elevation,
                                        double morale) {
        if (strength <= 0) {
            return 0.0;
        }
        double power = strength;
        power *= PostureMath.combatAggression(posture);
        power *= (1.0 + ELEVATION_BONUS_PER_BAND * elevation);
        // Morale in [0.5, 1.5]: well-fed troops fight harder, starving ones falter.
        double m = 0.5 + Math.max(0.0, Math.min(1.0, morale));
        power *= m;
        return power;
    }

    /**
     * Morale factor input (0..1) from food carried vs men. Full ration (>=1 food per
     * man) => 1.0; empty larder => 0.0.
     */
    public static double morale(int food, int strength) {
        if (strength <= 0) {
            return 0.0;
        }
        double ratio = (double) food / (double) strength;
        if (ratio > 1.0) {
            ratio = 1.0;
        }
        if (ratio < 0.0) {
            ratio = 0.0;
        }
        return ratio;
    }

    /**
     * Casualties inflicted on the side with {@code ownPower} by the opponent with
     * {@code enemyPower} this tick. Proportional to the power ratio and the side's
     * current strength. At least 1 when out-powered and still standing.
     */
    public static int casualties(double ownPower, double enemyPower, int ownStrength) {
        if (ownStrength <= 0) {
            return 0;
        }
        if (ownPower <= 0.0) {
            return ownStrength; // annihilated
        }
        double ratio = enemyPower / ownPower;
        int hit = (int) Math.round(ownStrength * BASE_CASUALTY_RATE * ratio);
        if (ratio > 1.0 && hit < 1) {
            hit = 1; // the losing side always bleeds
        }
        if (hit < 0) {
            hit = 0;
        }
        return Math.min(hit, ownStrength);
    }
}
