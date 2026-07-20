package com.heroquest.model;

/** Immutable grid coordinate. */
public final class Point {
    public final int x;
    public final int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point translate(int dx, int dy) {
        return new Point(x + dx, y + dy);
    }

    /** Chebyshev (king-move) distance. */
    public int chebyshev(Point o) {
        return Math.max(Math.abs(x - o.x), Math.abs(y - o.y));
    }

    /** Manhattan distance. */
    public int manhattan(Point o) {
        return Math.abs(x - o.x) + Math.abs(y - o.y);
    }

    public boolean isOrthogonalNeighbour(Point o) {
        return manhattan(o) == 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Point)) {
            return false;
        }
        Point p = (Point) obj;
        return x == p.x && y == p.y;
    }

    @Override
    public int hashCode() {
        return x * 31 + y;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
