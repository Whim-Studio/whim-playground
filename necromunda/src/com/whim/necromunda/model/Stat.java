package com.whim.necromunda.model;

/**
 * The nine core stats of a fighter's profile. Every value in the game's
 * mechanics keys off one of these. Kept as an enum so a {@link StatLine}
 * can be a compact, type-safe {@code EnumMap}.
 */
public enum Stat {
    M("Movement"),
    WS("Weapon Skill"),
    BS("Ballistic Skill"),
    S("Strength"),
    T("Toughness"),
    W("Wounds"),
    I("Initiative"),
    A("Attacks"),
    LD("Leadership");

    private final String fullName;

    Stat(String fullName) {
        this.fullName = fullName;
    }

    public String fullName() {
        return fullName;
    }
}
