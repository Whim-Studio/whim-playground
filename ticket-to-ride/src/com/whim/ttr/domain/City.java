package com.whim.ttr.domain;

/**
 * A city node on the board. Immutable. Layout coordinates live in a virtual
 * 0..1000 square that the UI scales to whatever panel size it renders into.
 */
public final class City {

    private final String id;
    private final String name;
    private final int x;
    private final int y;

    public City(String id, String name, int x, int y) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
    }

    public String id() { return id; }
    public String name() { return name; }
    public int x() { return x; }
    public int y() { return y; }

    @Override public String toString() { return id; }
}
