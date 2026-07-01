package com.whim.warroom.domain;

import java.awt.Color;

/** The two combatant sides plus a neutral bucket. */
public enum Faction {
    BLUE(new Color(70, 130, 220)),
    RED(new Color(210, 70, 60)),
    NEUTRAL(new Color(150, 150, 150));

    private final Color color;

    Faction(Color color) {
        this.color = color;
    }

    public Color color() {
        return color;
    }

    /** BLUE and RED are enemies; NEUTRAL is its own enemy (i.e. no side). */
    public Faction enemyOf() {
        switch (this) {
            case BLUE:
                return RED;
            case RED:
                return BLUE;
            default:
                return NEUTRAL;
        }
    }
}
