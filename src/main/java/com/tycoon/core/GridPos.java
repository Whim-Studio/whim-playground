package com.tycoon.core;
// LOCAL-ONLY VERIFICATION STUB (Task 1 owns this) — never committed by Task 2.
public final class GridPos {
    private final int x, y;
    private GridPos(int x, int y) { this.x = x; this.y = y; }
    public static GridPos of(int x, int y) { return new GridPos(x, y); }
    public int x() { return x; }
    public int y() { return y; }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GridPos)) return false;
        GridPos g = (GridPos) o;
        return x == g.x && y == g.y;
    }
    @Override public int hashCode() { return 31 * x + y; }
    @Override public String toString() { return "(" + x + "," + y + ")"; }
}
