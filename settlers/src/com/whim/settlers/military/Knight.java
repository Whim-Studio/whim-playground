package com.whim.settlers.military;

/**
 * A soldier garrisoned in a military building. Knights hold one of five ranks and
 * gain experience toward promotion — faster when trained in the Castle than
 * on-station, per the design. Combat strength scales with rank.
 */
public final class Knight {

    public static final int MAX_RANK = 5;
    private static final float PROMOTE_XP = 20f; // seconds of training per rank // approximate

    private int rank;
    private float xp;

    public Knight(int rank) {
        this.rank = clampRank(rank);
    }

    public int rank()     { return rank; }
    public int strength() { return rank; }

    /** Accumulate training; promotes on reaching the threshold. */
    public void train(float dt, boolean inCastle) {
        if (rank >= MAX_RANK) return;
        xp += dt * (inCastle ? 2f : 1f);
        if (xp >= PROMOTE_XP) { xp = 0f; rank++; }
    }

    private static int clampRank(int r) {
        return r < 1 ? 1 : (r > MAX_RANK ? MAX_RANK : r);
    }
}
