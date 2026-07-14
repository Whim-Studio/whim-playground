package com.whim.necromunda.model;

/**
 * A fighter's in-battle condition. Drives eligibility for actions and the
 * bottle/nerve thresholds. Campaign lasting-injuries live separately.
 */
public enum FighterStatus {
    /** Upright and able to act normally. */
    ACTIVE("Active"),
    /** Prone from incoming fire; must pass a recovery test to act again. */
    PINNED("Pinned"),
    /** Knocked down / seriously hurt; can crawl but not fight or shoot normally. */
    DOWN("Down"),
    /** Removed from this battle (rolls on the lasting-injury table post-game). */
    OUT_OF_ACTION("Out of Action"),
    /** Broke and left the field after a failed nerve test. */
    FLED("Fled");

    private final String label;

    FighterStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Whether a fighter in this status still counts as "in play" on the board. */
    public boolean inPlay() {
        return this != OUT_OF_ACTION && this != FLED;
    }
}
