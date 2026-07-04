package com.rampart.model;

/**
 * The polyomino shapes dealt to the player during the REPAIR phase. These mirror
 * the classic Tetris tetrominoes plus a one-cell {@link #DOT} used to patch tiny
 * gaps. Each shape's base cell layout (rotation 0) is defined in {@link WallPiece},
 * which precomputes the four rotation states from it.
 */
public enum WallShape {
    /** Single cell. */
    DOT,
    /** Four cells in a straight line. */
    I,
    /** Two-by-two square. */
    O,
    /** T-tetromino. */
    T,
    /** L-tetromino. */
    L,
    /** J-tetromino (mirror of L). */
    J,
    /** S-tetromino. */
    S,
    /** Z-tetromino (mirror of S). */
    Z
}
