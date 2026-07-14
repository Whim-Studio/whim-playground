package com.whim.necromunda.model;

/**
 * A fighter's role within the gang. Roles gate roster composition (exactly one
 * Leader, a capped number of Champions, Juves capped relative to Gangers) and
 * influence experience/advancement rates in a campaign.
 */
public enum FighterType {
    LEADER("Leader"),
    CHAMPION("Gang Champion"),
    GANGER("Ganger"),
    JUVE("Juve");

    private final String label;

    FighterType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
