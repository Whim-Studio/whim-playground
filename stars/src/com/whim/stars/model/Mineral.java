package com.whim.stars.model;

/**
 * The three minerals in Stars!. Ironium, Boranium and Germanium are mined from
 * planets, stored as cargo, and consumed (together with resources) to build
 * everything in the production queue.
 */
public enum Mineral {
    IRONIUM("Ironium"),
    BORANIUM("Boranium"),
    GERMANIUM("Germanium");

    private final String label;

    Mineral(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
