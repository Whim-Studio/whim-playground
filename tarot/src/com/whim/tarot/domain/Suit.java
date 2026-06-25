package com.whim.tarot.domain;

/** Suit also identifies the Major Arcana group. */
public enum Suit {
    MAJOR("Major Arcana"), WANDS("Wands"), CUPS("Cups"),
    SWORDS("Swords"), PENTACLES("Pentacles");

    private final String label;

    Suit(String label) { this.label = label; }

    public String getLabel() { return label; }

    public boolean isMajor() { return this == MAJOR; }
}
