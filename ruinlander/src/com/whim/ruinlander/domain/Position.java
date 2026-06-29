package com.whim.ruinlander.domain;

/** Immutable integer grid coordinate value object. */
public final class Position {
    public final int x, y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** 4-neighbour Manhattan distance. */
    public int manhattan(Position o) {
        return Math.abs(x - o.x) + Math.abs(y - o.y);
    }

    /** True when {@code o} is exactly one step away in a cardinal direction. */
    public boolean isAdjacent(Position o) {
        return manhattan(o) == 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Position)) return false;
        Position p = (Position) obj;
        return x == p.x && y == p.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
