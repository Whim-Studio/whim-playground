package com.whim.nextrun.domain;

/** Immutable grid coordinate. */
public final class Position {
    public final int x;
    public final int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Position translate(int dx, int dy) {
        return new Position(x + dx, y + dy);
    }

    public int manhattan(Position o) {
        return Math.abs(x - o.x) + Math.abs(y - o.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position)) return false;
        Position p = (Position) o;
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
