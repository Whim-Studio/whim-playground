package com.whim.necromunda.engine.rules;

/**
 * The three injury bands rolled when a fighter with no wounds left is hit.
 * (Standardized on the 1995 box: 1-2 / 3-4 / 5-6.)
 */
public enum InjuryResult {
    /** 1-2: a graze — the fighter stays up but takes a -1 stat knock. */
    FLESH_WOUND("Flesh Wound"),
    /** 3-4: knocked down / prone — out of the fight temporarily, may recover. */
    DOWN("Down"),
    /** 5-6: removed from the battle (rolls on the lasting-injury table post-game). */
    OUT_OF_ACTION("Out of Action");

    private final String label;

    InjuryResult(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
