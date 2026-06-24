package com.whim.coda.model;

/** A negative trait (Flaw). */
public class Flaw {

    private final String name;
    private final String description;

    public Flaw(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name;
    }
}
