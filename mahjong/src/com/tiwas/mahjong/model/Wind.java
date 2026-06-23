package com.tiwas.mahjong.model;

/**
 * The four winds, used both for seat winds and the prevailing round wind.
 * Ordered EAST, SOUTH, WEST, NORTH which is the clockwise rotation order.
 */
public enum Wind {
    EAST("East"),
    SOUTH("South"),
    WEST("West"),
    NORTH("North");

    private final String label;

    Wind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** The next wind in clockwise rotation (EAST -> SOUTH -> WEST -> NORTH -> EAST). */
    public Wind next() {
        return values()[(ordinal() + 1) % 4];
    }
}
