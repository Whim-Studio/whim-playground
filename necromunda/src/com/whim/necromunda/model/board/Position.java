package com.whim.necromunda.model.board;

/**
 * An immutable board coordinate: {@code (x, y)} tile plus a {@code z} level for
 * the vertical underhive. Value-equality makes it safe as a map key.
 */
public final class Position {

    private final int x;
    private final int y;
    private final int z;

    public Position(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Position(int x, int y) {
        this(x, y, 0);
    }

    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }

    public Position withZ(int newZ) {
        return new Position(x, y, newZ);
    }

    /** Straight-line ground distance to another position (ignores level). */
    public double planarDistance(Position other) {
        int dx = x - other.x;
        int dy = y - other.y;
        return Math.sqrt((double) (dx * dx + dy * dy));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Position)) {
            return false;
        }
        Position p = (Position) o;
        return x == p.x && y == p.y && z == p.z;
    }

    @Override
    public int hashCode() {
        int h = x;
        h = 31 * h + y;
        h = 31 * h + z;
        return h;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ",z" + z + ")";
    }
}
