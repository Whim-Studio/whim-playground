package com.rampart.model;

/**
 * An immutable integer grid coordinate expressed as {@code (col, row)}. Column is
 * the x-axis (east positive) and row is the y-axis (south positive). Value type:
 * safe to use as a {@link java.util.HashMap} key or {@link java.util.HashSet}
 * member.
 */
public final class Coord {
    private final int col;
    private final int row;

    /**
     * Creates a coordinate.
     *
     * @param col column index (x)
     * @param row row index (y)
     */
    public Coord(int col, int row) {
        this.col = col;
        this.row = row;
    }

    /** @return the column (x) index */
    public int col() { return col; }

    /** @return the row (y) index */
    public int row() { return row; }

    /**
     * Returns a new coordinate offset by the given deltas. Does not mutate this
     * instance.
     *
     * @param dCol column delta
     * @param dRow row delta
     * @return the translated coordinate
     */
    public Coord translate(int dCol, int dRow) {
        return new Coord(col + dCol, row + dRow);
    }

    /**
     * Returns the neighbour one cell away in the given direction.
     *
     * @param dir the direction to step
     * @return the adjacent coordinate
     */
    public Coord step(Direction dir) {
        return new Coord(col + dir.dCol(), row + dir.dRow());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coord)) return false;
        Coord c = (Coord) o;
        return col == c.col && row == c.row;
    }

    @Override
    public int hashCode() {
        return 31 * col + row;
    }

    @Override
    public String toString() {
        return "(" + col + "," + row + ")";
    }
}
