package com.whim.warroom.domain;

/** Historical era grouping for unit archetypes. */
public enum Era {
    ANTIQUITY("Antiquity"),
    MEDIEVAL("Medieval"),
    MODERN("Modern");

    private final String label;

    Era(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
