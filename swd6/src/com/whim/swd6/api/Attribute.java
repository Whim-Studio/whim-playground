package com.whim.swd6.api;

/**
 * The six D6-system attributes. Ordering here is the canonical sheet order.
 * Owned by the orchestrator (api).
 */
public enum Attribute {
    DEXTERITY("Dexterity", "DEX"),
    KNOWLEDGE("Knowledge", "KNO"),
    MECHANICAL("Mechanical", "MEC"),
    PERCEPTION("Perception", "PER"),
    STRENGTH("Strength", "STR"),
    TECHNICAL("Technical", "TEC");

    private final String display;
    private final String abbrev;

    Attribute(String display, String abbrev) {
        this.display = display;
        this.abbrev = abbrev;
    }

    public String display() {
        return display;
    }

    public String abbrev() {
        return abbrev;
    }
}
