package com.tiwa.mahjong.api;

/**
 * The three dragons. For a {@link Tile} of suit {@link Suit#DRAGON} the rank maps to
 * {@code ordinal() + 1} (Red=1, Green=2, White=3).
 */
public enum Dragon {
    RED, GREEN, WHITE;

    /** 1-based rank used by {@link Tile#getRank()} for dragon tiles. */
    public int rank() {
        return ordinal() + 1;
    }
}
