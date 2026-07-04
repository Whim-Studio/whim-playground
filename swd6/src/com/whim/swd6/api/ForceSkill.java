package com.whim.swd6.api;

/**
 * The three Force skills available to Force-Sensitive characters.
 * These are treated separately from attribute-based skills: they begin at 0D
 * and are only usable when the character is flagged Force-Sensitive.
 * Owned by the orchestrator (api).
 */
public enum ForceSkill {
    CONTROL("Control"),
    SENSE("Sense"),
    ALTER("Alter");

    private final String display;

    ForceSkill(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }
}
