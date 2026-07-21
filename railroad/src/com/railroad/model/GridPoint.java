package com.railroad.model;

/**
 * An immutable integer tile coordinate. Used as a graph-node key for the track
 * network and route paths. Value-equality so points can live in hash sets/maps.
 */
public final class GridPoint {

    public final int x;
    public final int y;

    public GridPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** Chebyshev-adjacent (including diagonals) and not the same tile. */
    public boolean isAdjacent(GridPoint other) {
        int dx = Math.abs(this.x - other.x);
        int dy = Math.abs(this.y - other.y);
        return (dx | dy) != 0 && dx <= 1 && dy <= 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GridPoint)) {
            return false;
        }
        GridPoint p = (GridPoint) o;
        return x == p.x && y == p.y;
    }

    @Override
    public int hashCode() {
        return x * 73856093 ^ y * 19349663;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
