package com.whim.necromunda.engine.rules;

/**
 * Armour saves. The target number is the armour's base save value worsened by
 * the weapon's armour-penetration (save modifier): {@code target = saveValue + AP}.
 * A target above 6 means no save is possible — and unlike to-hit/to-wound, a
 * natural 6 does <em>not</em> auto-save.
 */
public final class ArmourSave {

    private ArmourSave() {
    }

    /**
     * Modified save target. {@code saveValue} of 7 (unarmoured) or any result
     * above 6 means the save cannot succeed.
     */
    public static int target(int saveValue, int armourPiercing) {
        return saveValue + armourPiercing;
    }

    /** Whether a die roll makes the save. No natural-6 auto-save. */
    public static boolean saves(int roll, int target) {
        return target <= 6 && roll >= target;
    }
}
