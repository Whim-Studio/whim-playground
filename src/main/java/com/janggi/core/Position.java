package com.janggi.core;

/**
 * Immutable board intersection. The board is 10 rows (0..9) x 9 columns (0..8);
 * pieces sit ON intersections, so row/col ARE the intersection indices.
 * Row 0 is the HAN home rank, row 9 is the CHO home rank.
 */
public final class Position {

    private final int row;
    private final int col;

    private Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public static Position of(int row, int col) {
        return new Position(row, col);
    }

    public int row() {
        return row;
    }

    public int col() {
        return col;
    }

    /** True if this intersection lies on the 10x9 board. */
    public boolean isOnBoard() {
        return row >= 0 && row <= 9 && col >= 0 && col <= 8;
    }

    /**
     * True if this intersection is inside the given side's 3x3 palace.
     * Palaces span columns 3..5; CHO palace rows 7..9, HAN palace rows 0..2.
     */
    public boolean isInPalace(Side side) {
        if (col < 3 || col > 5) {
            return false;
        }
        if (side == Side.CHO) {
            return row >= 7 && row <= 9;
        }
        return row >= 0 && row <= 2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Position)) {
            return false;
        }
        Position other = (Position) o;
        return row == other.row && col == other.col;
    }

    @Override
    public int hashCode() {
        return row * 31 + col;
    }

    @Override
    public String toString() {
        return row + "," + col;
    }
}
