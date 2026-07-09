package com.whim.samurai.engine;

import java.util.List;
import java.util.Random;

/** Thin wrapper over {@link Random} with helpers used across the game. */
public class Rng {
    private final Random r;

    public Rng() { this.r = new Random(); }
    public Rng(long seed) { this.r = new Random(seed); }

    public int nextInt(int bound) { return r.nextInt(bound); }
    /** Inclusive range [lo, hi]. */
    public int range(int lo, int hi) { return lo + r.nextInt(Math.max(1, hi - lo + 1)); }
    public boolean chance(double p) { return r.nextDouble() < p; }
    public double nextDouble() { return r.nextDouble(); }
    public <T> T pick(List<T> xs) { return xs.get(r.nextInt(xs.size())); }
    public <T> T pick(T[] xs) { return xs[r.nextInt(xs.length)]; }
    /** Simulate a die roll of n dice with s sides (e.g. 2d6). */
    public int dice(int n, int sides) {
        int t = 0;
        for (int i = 0; i < n; i++) t += 1 + r.nextInt(sides);
        return t;
    }
}
