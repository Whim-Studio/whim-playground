package com.janggi.core;

/** Mutable 10x9 grid the engine manipulates; copyable for search. */
public class Board {

    public static final int ROWS = 10;
    public static final int COLS = 9;

    private final Piece[][] grid;

    public Board() {
        this.grid = new Piece[ROWS][COLS];
    }

    private Board(Piece[][] grid) {
        this.grid = grid;
    }

    public Piece pieceAt(Position p) {
        return pieceAt(p.row(), p.col());
    }

    public Piece pieceAt(int row, int col) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) {
            return null;
        }
        return grid[row][col];
    }

    /** Place {@code piece} at {@code p}; {@code null} clears the intersection. */
    public void set(Position p, Piece piece) {
        grid[p.row()][p.col()] = piece;
    }

    public Board copy() {
        Piece[][] g = new Piece[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            System.arraycopy(grid[r], 0, g[r], 0, COLS);
        }
        return new Board(g);
    }

    /** Locate the given side's General, or {@code null} if it has been captured. */
    public Position findGeneral(Side side) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                Piece p = grid[r][c];
                if (p != null && p.side() == side && p.type() == PieceType.GENERAL) {
                    return Position.of(r, c);
                }
            }
        }
        return null;
    }
}
