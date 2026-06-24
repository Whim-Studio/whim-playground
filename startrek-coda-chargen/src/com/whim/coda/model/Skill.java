package com.whim.coda.model;

/** A Coda skill keyed to a governing attribute. */
public class Skill {

    private final String name;
    private final Attribute key;

    public Skill(String name, Attribute key) {
        this.name = name;
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public Attribute getKey() {
        return key;
    }

    @Override
    public String toString() {
        return name;
    }
}
