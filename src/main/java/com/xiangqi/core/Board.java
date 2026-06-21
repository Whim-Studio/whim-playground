package com.xiangqi.core;

/**
 * A mutable 10x9 grid of pieces. The engine manipulates a Board directly and
 * copies it for search. Empty intersections hold {@code null}.
 */
public class Board {

    public static final int ROWS = 10;
    public static final int COLS = 9;

    private final Piece[][] grid;

    public Board() {
        this.grid = new Piece[ROWS][COLS];
    }

    /** The piece at {@code p}, or {@code null} if empty or off-board. */
    public Piece pieceAt(Position p) {
        return pieceAt(p.row(), p.col());
    }

    /** The piece at {@code (row, col)}, or {@code null} if empty or off-board. */
    public Piece pieceAt(int row, int col) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) {
            return null;
        }
        return grid[row][col];
    }

    /** Place {@code piece} at {@code p}; pass {@code null} to clear the square. */
    public void set(Position p, Piece piece) {
        grid[p.row()][p.col()] = piece;
    }

    /** A deep copy that shares no mutable state with this board. */
    public Board copy() {
        Board clone = new Board();
        for (int r = 0; r < ROWS; r++) {
            System.arraycopy(grid[r], 0, clone.grid[r], 0, COLS);
        }
        return clone;
    }

    /** The position of {@code side}'s General, or {@code null} if it has been captured. */
    public Position findGeneral(Side side) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                Piece piece = grid[r][c];
                if (piece != null && piece.type() == PieceType.GENERAL && piece.side() == side) {
                    return Position.of(r, c);
                }
            }
        }
        return null;
    }
}
