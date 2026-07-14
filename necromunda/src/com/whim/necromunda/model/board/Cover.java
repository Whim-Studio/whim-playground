package com.whim.necromunda.model.board;

/**
 * Cover level. In the 1995 rules cover is a <em>to-hit penalty</em> on the
 * shooter, not a save bonus — so each level carries the negative modifier it
 * imposes.
 */
public enum Cover {
    NONE("None", 0),
    PARTIAL("Partial", -1),
    HARD("Hard", -2);

    private final String label;
    private final int toHitModifier;

    Cover(String label, int toHitModifier) {
        this.label = label;
        this.toHitModifier = toHitModifier;
    }

    public String label() { return label; }

    /** The (negative) to-hit modifier this cover imposes on a shooter. */
    public int toHitModifier() { return toHitModifier; }
}
