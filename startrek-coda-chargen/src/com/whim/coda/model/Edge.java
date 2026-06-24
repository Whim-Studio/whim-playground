package com.whim.coda.model;

/** A positive trait (Edge). */
public class Edge {

    private final String name;
    private final String description;

    public Edge(String name, String description) {
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
