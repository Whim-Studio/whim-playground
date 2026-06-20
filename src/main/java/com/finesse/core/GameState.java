package com.finesse.core;

/**
 * The full mutable state of a game in progress: the {@link Board} plus turn
 * tracking and move counters.
 *
 * <p>The no-arg constructor yields a game with an <em>empty</em> standard-sized
 * board and {@link PieceColor#WHITE} to move. The core deliberately does not
 * place any pieces &mdash; populating the Finesse starting position is the job of
 * the variant's setup class, which can call {@link Board#setPiece} on
 * {@link #getBoard()}.
 */
public final class GameState {

    private final Board board;
    private PieceColor sideToMove;
    private int fullMoveNumber;
    private int halfMoveClock;

    /**
     * Creates a game state with an empty 8x8 board, White to move, at move 1.
     */
    public GameState() {
        this(new Board(), PieceColor.WHITE, 1, 0);
    }

    /**
     * Creates a game state wrapping the given board.
     *
     * @param board      the board to wrap; must not be {@code null}
     * @param sideToMove the side to move; must not be {@code null}
     */
    public GameState(Board board, PieceColor sideToMove) {
        this(board, sideToMove, 1, 0);
    }

    /**
     * Creates a fully specified game state.
     *
     * @param board          the board to wrap; must not be {@code null}
     * @param sideToMove     the side to move; must not be {@code null}
     * @param fullMoveNumber the full-move counter (starts at 1, increments after Black moves)
     * @param halfMoveClock  the half-move clock (plies since the last capture or pawn move)
     * @throws NullPointerException if {@code board} or {@code sideToMove} is {@code null}
     */
    public GameState(Board board, PieceColor sideToMove, int fullMoveNumber, int halfMoveClock) {
        if (board == null) {
            throw new NullPointerException("board");
        }
        if (sideToMove == null) {
            throw new NullPointerException("sideToMove");
        }
        this.board = board;
        this.sideToMove = sideToMove;
        this.fullMoveNumber = fullMoveNumber;
        this.halfMoveClock = halfMoveClock;
    }

    /** @return the board (mutable; the same instance held by this state). */
    public Board getBoard() {
        return board;
    }

    /** @return the side whose turn it is to move. */
    public PieceColor getSideToMove() {
        return sideToMove;
    }

    /**
     * Sets the side to move.
     *
     * @param sideToMove the new side to move; must not be {@code null}
     * @throws NullPointerException if {@code sideToMove} is {@code null}
     */
    public void setSideToMove(PieceColor sideToMove) {
        if (sideToMove == null) {
            throw new NullPointerException("sideToMove");
        }
        this.sideToMove = sideToMove;
    }

    /**
     * Flips the side to move to the opponent. Convenience for turn advancement;
     * callers that also track move counters are responsible for updating those.
     */
    public void toggleSideToMove() {
        this.sideToMove = sideToMove.opponent();
    }

    /** @return the full-move counter (increments after Black's move). */
    public int getFullMoveNumber() {
        return fullMoveNumber;
    }

    /** @param fullMoveNumber the new full-move counter. */
    public void setFullMoveNumber(int fullMoveNumber) {
        this.fullMoveNumber = fullMoveNumber;
    }

    /** @return the half-move clock (plies since the last capture or pawn move). */
    public int getHalfMoveClock() {
        return halfMoveClock;
    }

    /** @param halfMoveClock the new half-move clock. */
    public void setHalfMoveClock(int halfMoveClock) {
        this.halfMoveClock = halfMoveClock;
    }

    /**
     * Creates a deep copy of this game state.
     *
     * <p>The board is deep-copied via {@link Board#copy()}, so the returned state
     * is fully independent and can be mutated (for example to explore a candidate
     * move) without affecting this one.
     *
     * @return an independent copy of this game state
     */
    public GameState copy() {
        return new GameState(board.copy(), sideToMove, fullMoveNumber, halfMoveClock);
    }
}
