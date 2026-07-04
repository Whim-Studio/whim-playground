package com.whim.swd6.api;

/**
 * Immutable outcome of a damage resolution: the damage roll, the resistance roll,
 * their margin, and the wound level inflicted by this single hit (before any
 * escalation against the target's existing wounds).
 *
 * Owned by the orchestrator (api).
 */
public final class DamageResult {

    private final RollResult damageRoll;
    private final RollResult resistRoll;
    private final int margin;            // damageTotal - resistTotal
    private final WoundLevel inflicted;

    public DamageResult(RollResult damageRoll, RollResult resistRoll, int margin, WoundLevel inflicted) {
        this.damageRoll = damageRoll;
        this.resistRoll = resistRoll;
        this.margin = margin;
        this.inflicted = inflicted;
    }

    public RollResult getDamageRoll() { return damageRoll; }
    public RollResult getResistRoll() { return resistRoll; }
    public int getMargin() { return margin; }
    public WoundLevel getInflicted() { return inflicted; }
}
