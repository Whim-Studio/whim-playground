package com.tycoon.core;

/**
 * Immutable grid coordinate value type.
 */
public final class GridPos {
    private final int x;
    private final int y;

    private GridPos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static GridPos of(int x, int y) {
        return new GridPos(x, y);
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GridPos)) {
            return false;
        }
        GridPos other = (GridPos) o;
        return x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
