package com.whim.stars.model;

/**
 * The six research fields of Stars!. Each field advances independently from
 * level 0 to {@link TechLevels#MAX_LEVEL}, unlocking components and buildings.
 */
public enum TechField {
    ENERGY("Energy"),
    WEAPONS("Weapons"),
    PROPULSION("Propulsion"),
    CONSTRUCTION("Construction"),
    ELECTRONICS("Electronics"),
    BIOTECHNOLOGY("Biotechnology");

    private final String label;

    TechField(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
