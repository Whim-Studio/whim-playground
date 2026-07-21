package com.railroad.ui;

/** The map-interaction tools available in Phase 1. */
public enum Tool {
    SELECT("Select"),
    BUILD_TRACK("Build Track");

    private final String label;

    Tool(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
