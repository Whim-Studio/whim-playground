package com.dglz.domain;

/** A play made by a seat in a trick. combo == null means PASS. */
public final class Play {
    private final int seat;
    private final Combination combo;

    public Play(int seat, Combination combo) {
        this.seat = seat;
        this.combo = combo;
    }

    public int seat() {
        return seat;
    }

    public Combination combo() {
        return combo;
    }

    public boolean isPass() {
        return combo == null;
    }
}
