package com.whim.necromunda.engine.rules;

/**
 * Ranged to-hit resolution. The base target number is {@code 7 - BS} (so BS4
 * hits on 3+, BS6 would be 1+ which clamps to 2+). To-hit modifiers are applied
 * where a <em>positive</em> modifier makes the shot easier (short range +1) and a
 * <em>negative</em> modifier makes it harder (long range -1, hard cover -2). A
 * natural 6 always hits regardless of the target.
 */
public final class RangedToHit {

    private RangedToHit() {
    }

    /** Base target with no modifiers: {@code clamp(7 - BS, 2, 6)}. */
    public static int baseTarget(int ballisticSkill) {
        return clampLow(7 - ballisticSkill);
    }

    /**
     * Target number after applying the net to-hit modifier (positive = easier).
     * Lower-clamped to 2 (you always miss on a 1); may exceed 6, in which case
     * only a natural 6 can hit.
     */
    public static int modifiedTarget(int ballisticSkill, int toHitModifier) {
        return clampLow((7 - ballisticSkill) - toHitModifier);
    }

    /** Whether a die roll hits a given target number (natural 6 always hits). */
    public static boolean hits(int roll, int target) {
        if (roll >= 6) {
            return true;
        }
        return target <= 6 && roll >= target;
    }

    private static int clampLow(int target) {
        return target < 2 ? 2 : target;
    }
}
