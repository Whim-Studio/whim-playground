package com.whim.swd6.api;

/**
 * Standard difficulty tiers and their target-number ranges (Revised &amp; Expanded).
 * A roll succeeds when its total meets or exceeds the target number in play.
 * {@link #representativeTarget()} gives a canonical mid-range number the GM/engine
 * can use when only a tier (not an exact number) is specified.
 *
 * Owned by the orchestrator (api).
 */
public enum DifficultyTier {
    VERY_EASY("Very Easy", 1, 5, 3),
    EASY("Easy", 6, 10, 8),
    MODERATE("Moderate", 11, 15, 13),
    DIFFICULT("Difficult", 16, 20, 18),
    VERY_DIFFICULT("Very Difficult", 21, 30, 25),
    HEROIC("Heroic", 31, Integer.MAX_VALUE, 33);

    private final String display;
    private final int min;
    private final int max;
    private final int representative;

    DifficultyTier(String display, int min, int max, int representative) {
        this.display = display;
        this.min = min;
        this.max = max;
        this.representative = representative;
    }

    public String display() {
        return display;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    /** A canonical target number for this tier when no exact number is given. */
    public int representativeTarget() {
        return representative;
    }

    /** Classify an exact target number into its tier. */
    public static DifficultyTier fromTarget(int target) {
        for (DifficultyTier t : values()) {
            if (target >= t.min && target <= t.max) {
                return t;
            }
        }
        return target < 1 ? VERY_EASY : HEROIC;
    }
}
