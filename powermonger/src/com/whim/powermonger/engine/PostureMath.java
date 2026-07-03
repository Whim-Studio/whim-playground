package com.whim.powermonger.engine;

import com.whim.powermonger.api.Enums.Posture;

/**
 * Posture scaling math. Pure functions over {@link Posture#scale()} (0.25 / 0.50 /
 * 1.00). Depends only on {@code api}. Used by the engine to scale the magnitude of
 * RECRUIT / GATHER_FOOD / combat aggression by the ordering bloc's posture.
 *
 * Faithful to Powermonger: an AGGRESSIVE order takes ~100% of what is available,
 * NEUTRAL ~50%, PASSIVE ~25%.
 */
public final class PostureMath {
    private PostureMath() {}

    /**
     * Number of recruits taken from a town of {@code eligible} population at the
     * given posture. AGGRESSIVE ~= all eligible, PASSIVE ~= a quarter. Always at
     * least 1 when eligible &gt; 0 and posture is not the very smallest slice.
     */
    public static int recruitCount(int eligible, Posture posture) {
        if (eligible <= 0) {
            return 0;
        }
        int taken = (int) Math.round(eligible * posture.scale());
        if (taken < 1 && posture.scale() >= 0.25 && eligible >= 1) {
            taken = 1;
        }
        return Math.min(taken, eligible);
    }

    /**
     * Amount of food looted from a node holding {@code available} food, scaled by
     * posture. GATHER_FOOD loots proportionally to aggression.
     */
    public static int lootAmount(int available, Posture posture) {
        if (available <= 0) {
            return 0;
        }
        int taken = (int) Math.round(available * posture.scale());
        if (taken < 1 && available >= 1) {
            taken = 1;
        }
        return Math.min(taken, available);
    }

    /**
     * Combat aggression multiplier for a bloc at the given posture. Aggressive
     * blocs commit fully (1.0); passive blocs hold back (0.25..). Applied on top of
     * raw strength when resolving a skirmish.
     */
    public static double combatAggression(Posture posture) {
        // Compress the range a little so a passive bloc still defends itself:
        // 0.25 -> 0.5, 0.50 -> 0.75, 1.00 -> 1.0.
        return 0.5 + 0.5 * posture.scale();
    }

    /** Generic proportional scale by posture, clamped to [0, available]. */
    public static int scaled(int available, Posture posture) {
        return lootAmount(available, posture);
    }
}
