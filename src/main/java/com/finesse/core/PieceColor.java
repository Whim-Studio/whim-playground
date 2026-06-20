package com.finesse.core;

/**
 * The two sides in a game.
 */
public enum PieceColor {
    WHITE,
    BLACK;

    /**
     * @return the opposing color.
     */
    public PieceColor opponent() {
        return this == WHITE ? BLACK : WHITE;
    }
}
