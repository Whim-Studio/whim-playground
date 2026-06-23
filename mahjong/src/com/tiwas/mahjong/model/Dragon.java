package com.tiwas.mahjong.model;

/** The three dragon honour tiles. */
public enum Dragon {
    RED("Red"),
    GREEN("Green"),
    WHITE("White");

    private final String label;

    Dragon(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
