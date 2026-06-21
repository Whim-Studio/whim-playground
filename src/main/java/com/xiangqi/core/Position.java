package com.xiangqi.core;

/**
 * An immutable board intersection. The board is 10 rows (0..9) by 9 columns
 * (0..8); pieces sit ON intersections, so {@code row}/{@code col} are the
 * intersection indices directly. Row 0 is BLACK's home rank (top), row 9 is
 * RED's home rank (bottom).
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

    /** True when this position lies on the 10x9 board. */
    public boolean isOnBoard() {
        return row >= 0 && row <= 9 && col >= 0 && col <= 8;
    }

    /** True when this position is inside the 3x3 palace belonging to {@code side}. */
    public boolean isInPalace(Side side) {
        if (col < 3 || col > 5) {
            return false;
        }
        if (side == Side.RED) {
            return row >= 7 && row <= 9;
        }
        return row >= 0 && row <= 2;
    }

    /**
     * True once this position is on the enemy half of the board (i.e. the given
     * side's piece here has crossed the River). RED's enemy half is rows 0..4;
     * BLACK's enemy half is rows 5..9.
     */
    public boolean isAcrossRiver(Side side) {
        if (side == Side.RED) {
            return row <= 4;
        }
        return row >= 5;
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
