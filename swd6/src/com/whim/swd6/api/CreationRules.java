package com.whim.swd6.api;

import java.util.Map;

/**
 * Shared character-creation constants and pure validation helpers (Revised &amp;
 * Expanded defaults). Kept in api so the creation wizard (Task 3) and the model /
 * rules layer (Task 1) validate against identical numbers.
 *
 * Units are "pips" (3 pips = 1 die) for exact integer math.
 *
 * Owned by the orchestrator (api).
 */
public final class CreationRules {

    private CreationRules() {
    }

    /** Total attribute dice to distribute in point-buy: 18D. */
    public static final int ATTRIBUTE_DICE_TOTAL = 18;
    public static final int ATTRIBUTE_PIPS_TOTAL = ATTRIBUTE_DICE_TOTAL * 3; // 54

    /** Per-attribute range for the default (human) build. */
    public static final DiceCode ATTRIBUTE_MIN = DiceCode.of(2, 0); // 2D
    public static final DiceCode ATTRIBUTE_MAX = DiceCode.of(4, 0); // 4D

    /** Skill dice to distribute over starting skills: 7D. */
    public static final int SKILL_DICE_TOTAL = 7;
    public static final int SKILL_PIPS_TOTAL = SKILL_DICE_TOTAL * 3; // 21

    /** Max dice that may be added to any single skill at creation: +2D. */
    public static final DiceCode SKILL_MAX_ADD_AT_CREATION = DiceCode.of(2, 0); // +2D

    /** Starting economies (R&E). */
    public static final int STARTING_FORCE_POINTS = 1;
    public static final int STARTING_CHARACTER_POINTS = 5;

    /** Sum of all attribute pips in the map. */
    public static int totalAttributePips(Map<Attribute, DiceCode> attrs) {
        int sum = 0;
        for (Attribute a : Attribute.values()) {
            DiceCode d = attrs.get(a);
            if (d != null) {
                sum += d.pipValue();
            }
        }
        return sum;
    }

    /** Attribute pips still available (may be negative if over-spent). */
    public static int attributePipsRemaining(Map<Attribute, DiceCode> attrs) {
        return ATTRIBUTE_PIPS_TOTAL - totalAttributePips(attrs);
    }

    /** True when an attribute code sits within the allowed 2D..4D window. */
    public static boolean attributeInRange(DiceCode code) {
        return code.pipValue() >= ATTRIBUTE_MIN.pipValue()
                && code.pipValue() <= ATTRIBUTE_MAX.pipValue();
    }

    /** Sum of all dice added over attributes across the given skills (in pips). */
    public static int totalSkillAddPips(Iterable<Skill> skills) {
        int sum = 0;
        for (Skill s : skills) {
            sum += s.getAdded().pipValue();
        }
        return sum;
    }

    /** Skill pips still available to distribute (may be negative if over-spent). */
    public static int skillPipsRemaining(Iterable<Skill> skills) {
        return SKILL_PIPS_TOTAL - totalSkillAddPips(skills);
    }

    /** True when a single skill's added dice respects the +2D creation cap. */
    public static boolean skillAddWithinCap(DiceCode added) {
        return added.pipValue() <= SKILL_MAX_ADD_AT_CREATION.pipValue();
    }
}
