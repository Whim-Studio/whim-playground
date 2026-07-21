package com.railroad.ui;

/** The map-interaction tools available to the player. */
public enum Tool {
    SELECT("Select"),
    BUILD_TRACK("Build Track"),
    BUILD_STATION("Build Station");

    private final String label;

    Tool(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
