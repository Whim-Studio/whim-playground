package com.whim.scg.api;

/** Immutable integer grid coordinate (used by ship interior + boarding grids). */
public final class GridPos {
    public final int x, y;

    public GridPos(int x, int y) { this.x = x; this.y = y; }

    public GridPos translate(int dx, int dy) { return new GridPos(x + dx, y + dy); }

    public GridPos step(Enums.Direction d) { return new GridPos(x + d.dx, y + d.dy); }

    public int manhattan(GridPos o) { return Math.abs(x - o.x) + Math.abs(y - o.y); }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GridPos)) return false;
        GridPos g = (GridPos) o;
        return x == g.x && y == g.y;
    }

    @Override public int hashCode() { return x * 31 + y; }

    @Override public String toString() { return "(" + x + "," + y + ")"; }
}
