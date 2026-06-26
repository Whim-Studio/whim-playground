package com.whim.jutsutrainer.domain;

/**
 * The chakra nature transformation a jutsu draws on.
 *
 * <p>The five basic elemental natures (Fire, Water, Lightning, Earth, Wind)
 * plus the two spiritual/physical natures (Yin, Yang) and their combination
 * (Yin–Yang). {@link #NONE} covers techniques with no nature transformation
 * (shape-transformation, sealing, taijutsu, etc.).
 */
public enum ChakraNature {
    FIRE("Fire"),
    WATER("Water"),
    LIGHTNING("Lightning"),
    EARTH("Earth"),
    WIND("Wind"),
    YIN("Yin"),
    YANG("Yang"),
    YIN_YANG("Yin–Yang"),
    NONE("None");

    private final String displayName;

    ChakraNature(String displayName) {
        this.displayName = displayName;
    }

    /** Human-readable name, e.g. {@code YIN_YANG -> "Yin–Yang"}, {@code NONE -> "None"}. */
    public String getDisplayName() {
        return displayName;
    }
}
