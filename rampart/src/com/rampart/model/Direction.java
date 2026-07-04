package com.rampart.model;

/**
 * Eight-way compass direction with unit column/row deltas. Useful to the engine
 * for ship headings, firing arcs, and neighbour scans. Pure data — no movement is
 * performed here.
 *
 * <p>Row increases downward (screen convention), so {@link #NORTH} has a delta of
 * {@code (0, -1)}.
 */
public enum Direction {
    NORTH(0, -1),
    NORTHEAST(1, -1),
    EAST(1, 0),
    SOUTHEAST(1, 1),
    SOUTH(0, 1),
    SOUTHWEST(-1, 1),
    WEST(-1, 0),
    NORTHWEST(-1, -1);

    private final int dCol;
    private final int dRow;

    Direction(int dCol, int dRow) {
        this.dCol = dCol;
        this.dRow = dRow;
    }

    /** Column delta (east positive). */
    public int dCol() { return dCol; }

    /** Row delta (south positive, north negative). */
    public int dRow() { return dRow; }

    /** The four orthogonal directions, in clockwise order from north. */
    public static Direction[] cardinals() {
        return new Direction[] { NORTH, EAST, SOUTH, WEST };
    }

    /** This direction rotated 90 degrees clockwise. */
    public Direction clockwise() {
        Direction[] v = values();
        return v[(ordinal() + 2) % v.length];
    }

    /** The direction directly opposite this one. */
    public Direction opposite() {
        Direction[] v = values();
        return v[(ordinal() + 4) % v.length];
    }
}
