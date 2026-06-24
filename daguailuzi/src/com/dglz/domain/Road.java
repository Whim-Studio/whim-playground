package com.dglz.domain;

/** The 4 playable paths (combination sizes). */
public enum Road {
    SINGLE(1), PAIR(2), TRIPLE(3), FIVE(5);

    private final int size;

    Road(int size) {
        this.size = size;
    }

    public int size() {
        return size;
    }

    /** null if n is not 1/2/3/5. */
    public static Road forSize(int n) {
        switch (n) {
            case 1:
                return SINGLE;
            case 2:
                return PAIR;
            case 3:
                return TRIPLE;
            case 5:
                return FIVE;
            default:
                return null;
        }
    }
}
