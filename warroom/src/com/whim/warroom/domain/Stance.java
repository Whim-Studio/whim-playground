package com.whim.warroom.domain;

/** Tactical posture that drives AI behavior and combat modifiers. */
public enum Stance {
    OFFENSIVE("Offensive"),
    DEFENSIVE("Defensive"),
    RETREAT("Retreat");

    private final String label;

    Stance(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
