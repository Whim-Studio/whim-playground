package com.whim.starcommand.engine;

import java.util.Random;

/** Seedable RNG wrapper so combat and generation are deterministic under test. */
public class Rng {
    private final Random random;

    public Rng() { this.random = new Random(); }
    public Rng(long seed) { this.random = new Random(seed); }

    /** Inclusive range [lo, hi]. */
    public int range(int lo, int hi) {
        if (hi <= lo) return lo;
        return lo + random.nextInt(hi - lo + 1);
    }

    /** Roll n dice of the given number of sides and sum them. */
    public int roll(int dice, int sides) {
        int total = 0;
        for (int i = 0; i < dice; i++) total += 1 + random.nextInt(sides);
        return total;
    }

    /** True with the given percent chance (0..100). */
    public boolean chance(int percent) {
        return random.nextInt(100) < percent;
    }

    public int nextInt(int bound) { return random.nextInt(bound); }
}
