package com.finesse.core;

/**
 * A rectangular grid of squares, each optionally holding a {@link Piece}.
 *
 * <p>The board is fully modular: its {@code width} and {@code height} are stored
 * as fields and default to the standard 8x8, but any positive dimensions are
 * supported. No method assumes an 8x8 board. Squares are addressed by
 * {@link Position}, where {@code file} runs {@code [0, width)} and {@code rank}
 * runs {@code [0, height)}.
 *
 * <p>This class stores grid state only. It does not know the rules of any
 * variant; legal move generation lives in {@link MoveGenerator}.
 */
public final class Board {

    /** Default board width for standard chess. */
    public static final int DEFAULT_WIDTH = 8;
    /** Default board height for standard chess. */
    public static final int DEFAULT_HEIGHT = 8;

    private final int width;
    private final int height;
    // Indexed as squares[rank][file]; null means an empty square.
    private final Piece[][] squares;

    /**
     * Creates an empty 8x8 board.
     */
    public Board() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Creates an empty board of the given dimensions.
     *
     * @param width  number of files (columns); must be positive
     * @param height number of ranks (rows); must be positive
     * @throws IllegalArgumentException if either dimension is not positive
     */
    public Board(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Board dimensions must be positive: " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        this.squares = new Piece[height][width];
    }

    /** @return the number of files (columns). */
    public int getWidth() {
        return width;
    }

    /** @return the number of ranks (rows). */
    public int getHeight() {
        return height;
    }

    /**
     * Tests whether a position lies on this board.
     *
     * @param pos the position to test; {@code null} is treated as out of bounds
     * @return {@code true} if the position is within bounds
     */
    public boolean isInBounds(Position pos) {
        if (pos == null) {
            return false;
        }
        int f = pos.getFile();
        int r = pos.getRank();
        return f >= 0 && f < width && r >= 0 && r < height;
    }

    /**
     * Returns the piece at a position.
     *
     * @param pos the square to read; must be in bounds
     * @return the piece there, or {@code null} if the square is empty
     * @throws IndexOutOfBoundsException if {@code pos} is out of bounds
     */
    public Piece getPiece(Position pos) {
        requireInBounds(pos);
        return squares[pos.getRank()][pos.getFile()];
    }

    /**
     * Places a piece on a square, overwriting any existing occupant.
     *
     * @param pos   the target square; must be in bounds
     * @param piece the piece to place; {@code null} clears the square
     * @throws IndexOutOfBoundsException if {@code pos} is out of bounds
     */
    public void setPiece(Position pos, Piece piece) {
        requireInBounds(pos);
        squares[pos.getRank()][pos.getFile()] = piece;
    }

    /**
     * Clears a square, returning whatever occupied it.
     *
     * @param pos the square to clear; must be in bounds
     * @return the removed piece, or {@code null} if the square was already empty
     * @throws IndexOutOfBoundsException if {@code pos} is out of bounds
     */
    public Piece removePiece(Position pos) {
        requireInBounds(pos);
        Piece previous = squares[pos.getRank()][pos.getFile()];
        squares[pos.getRank()][pos.getFile()] = null;
        return previous;
    }

    /**
     * Creates a deep copy of this board.
     *
     * <p>The returned board has the same dimensions and the same piece placement.
     * Because {@link Piece} instances are immutable they are shared rather than
     * cloned, but the grid array itself is fully independent, so mutating the
     * copy never affects the original.
     *
     * @return an independent copy of this board
     */
    public Board copy() {
        Board clone = new Board(width, height);
        for (int r = 0; r < height; r++) {
            System.arraycopy(squares[r], 0, clone.squares[r], 0, width);
        }
        return clone;
    }

    private void requireInBounds(Position pos) {
        if (!isInBounds(pos)) {
            throw new IndexOutOfBoundsException(
                    "Position out of bounds for " + width + "x" + height + " board: " + pos);
        }
    }
}
