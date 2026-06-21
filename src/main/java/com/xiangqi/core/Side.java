package com.xiangqi.core;

/**
 * The two players. RED sits at the bottom (rows 5..9) and moves first; BLACK
 * sits at the top (rows 0..4).
 */
public enum Side {
    RED,
    BLACK;

    /** The other side. */
    public Side opponent() {
        return this == RED ? BLACK : RED;
    }

    /**
     * The row delta of a "forward" step for this side. RED advances toward
     * row 0 ({@code -1}); BLACK advances toward row 9 ({@code +1}).
     */
    public int forward() {
        return this == RED ? -1 : +1;
    }
}
