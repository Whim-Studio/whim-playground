package com.whim.cardwoven.domain;

/**
 * Deterministic monotonically-increasing id source shared while building a deck
 * or a game so every {@link Card} / {@link Building} gets a unique id. Not
 * threaded; the domain layer is single-threaded model state.
 */
public final class IdGenerator {
    private int next;

    public IdGenerator() {
        this(1);
    }

    public IdGenerator(int first) {
        this.next = first;
    }

    /** Returns the next unique id. */
    public int next() {
        return next++;
    }

    /** The id that will be returned by the next call to {@link #next()}. */
    public int peek() {
        return next;
    }
}
