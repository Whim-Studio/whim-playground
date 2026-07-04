package com.whim.swd6.api;

/**
 * The damage/wound ladder (Revised &amp; Expanded).
 *
 * Ordinal order is severity order. Each level carries the standard skill/attribute
 * penalty applied while at that level. Damage is resolved by comparing a damage
 * roll against a resistance (Strength + armor) roll; the margin maps to a wound
 * level via {@link #fromDamageMargin(int)}. Taking a wound while already wounded
 * escalates via {@link #escalate(WoundLevel)} (two Wounded results = Incapacitated,
 * two Stunned = Wounded, etc.).
 *
 * Owned by the orchestrator (api).
 */
public enum WoundLevel {
    HEALTHY("Healthy", 0),
    STUNNED("Stunned", 1),          // -1D to actions until end of next round
    WOUNDED("Wounded", 1),          // -1D to all actions
    WOUNDED_TWICE("Wounded Twice", 2), // -2D to all actions
    INCAPACITATED("Incapacitated", 0), // knocked out / prone, cannot act
    MORTALLY_WOUNDED("Mortally Wounded", 0), // dying; unconscious
    KILLED("Killed", 0);

    private final String display;
    private final int penaltyDice;

    WoundLevel(String display, int penaltyDice) {
        this.display = display;
        this.penaltyDice = penaltyDice;
    }

    public String display() {
        return display;
    }

    /** Number of dice subtracted from actions while at this level. */
    public int penaltyDice() {
        return penaltyDice;
    }

    /** True when the character can no longer take actions. */
    public boolean incapacitatedOrWorse() {
        return ordinal() >= INCAPACITATED.ordinal();
    }

    /**
     * Map a damage margin (damageTotal - resistTotal) to the wound inflicted by a
     * single hit (Revised &amp; Expanded table).
     */
    public static WoundLevel fromDamageMargin(int margin) {
        if (margin < 0) {
            return HEALTHY;         // resisted, no effect
        }
        if (margin <= 3) {
            return STUNNED;
        }
        if (margin <= 8) {
            return WOUNDED;
        }
        if (margin <= 12) {
            return INCAPACITATED;
        }
        if (margin <= 15) {
            return MORTALLY_WOUNDED;
        }
        return KILLED;
    }

    /**
     * Combine a currently-held wound level with a newly inflicted one, applying the
     * R&amp;E stacking rules. Returns the resulting (never lower) level.
     *
     * Stacking: two Stunned -> Wounded; Wounded + Stunned -> stays Wounded;
     * Wounded + Wounded -> Incapacitated; anything at/above Incapacitated stays as
     * the worse of the two.
     */
    public WoundLevel escalate(WoundLevel inflicted) {
        if (inflicted == HEALTHY) {
            return this;
        }
        if (this == HEALTHY) {
            return inflicted;
        }
        // Stunned is transient; a fresh Stunned on top of Stunned becomes Wounded.
        if (this == STUNNED && inflicted == STUNNED) {
            return WOUNDED;
        }
        if (this == WOUNDED && inflicted == WOUNDED) {
            return INCAPACITATED;
        }
        if (this == WOUNDED && inflicted == STUNNED) {
            return WOUNDED;
        }
        if (this == STUNNED && inflicted == WOUNDED) {
            return WOUNDED;
        }
        if (this == WOUNDED_TWICE && (inflicted == WOUNDED || inflicted == STUNNED)) {
            return INCAPACITATED;
        }
        // Otherwise the worse of the two levels wins.
        return this.ordinal() >= inflicted.ordinal() ? this : inflicted;
    }
}
