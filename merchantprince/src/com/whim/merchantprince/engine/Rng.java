package com.whim.merchantprince.engine;

import java.util.List;
import java.util.Random;

/** Thin wrapper over {@link Random} with helpers used across the game. */
public class Rng {
    private final Random r;

    public Rng() { this.r = new Random(); }
    public Rng(long seed) { this.r = new Random(seed); }

    public int nextInt(int bound) { return r.nextInt(Math.max(1, bound)); }
    /** Inclusive range [lo, hi]. */
    public int range(int lo, int hi) { return lo + r.nextInt(Math.max(1, hi - lo + 1)); }
    public boolean chance(double p) { return r.nextDouble() < p; }
    public double nextDouble() { return r.nextDouble(); }
    public double gaussian() { return r.nextGaussian(); }
    public <T> T pick(List<T> xs) { return xs.get(r.nextInt(xs.size())); }
    public <T> T pick(T[] xs) { return xs[r.nextInt(xs.length)]; }
}
