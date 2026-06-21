package com.whim.hbdi.domain;

/**
 * The four HBDI thinking-style quadrants.
 * A=Analytical, B=Sequential, C=Interpersonal, D=Imaginative.
 */
public enum Quadrant {
    A("Analytical"), B("Sequential"), C("Interpersonal"), D("Imaginative");

    private final String label;

    Quadrant(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
