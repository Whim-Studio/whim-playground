package com.midnight.core;

/**
 * An immutable tile coordinate on the world {@link Map}. {@code x} runs west to
 * east (0..width-1) and {@code y} runs north to south (0..height-1, with y=0 the
 * northern edge). Value equality is by {@code (x, y)}.
 */
public final class Location {

    private final int x;
    private final int y;

    private Location(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static Location of(int x, int y) {
        return new Location(x, y);
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    /** The adjacent tile one step in direction {@code d} (x+dx, y+dy). */
    public Location neighbor(Direction d) {
        return new Location(x + d.dx(), y + d.dy());
    }

    /** The Chebyshev (king-move) distance to {@code other}: max of the axis gaps. */
    public int chebyshevDistanceTo(Location other) {
        return Math.max(Math.abs(x - other.x), Math.abs(y - other.y));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Location)) {
            return false;
        }
        Location other = (Location) o;
        return x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
        return x * 31 + y;
    }

    @Override
    public String toString() {
        return x + "," + y;
    }
}
