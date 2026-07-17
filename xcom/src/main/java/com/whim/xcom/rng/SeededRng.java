package com.whim.xcom.rng;

import java.util.Random;

/**
 * Default {@link Rng} backed by {@link java.util.Random} (a linear congruential
 * generator). Same seed → identical sequence on every JVM, which is exactly the
 * determinism the rules layer relies on.
 */
public final class SeededRng implements Rng {

    private final long seed;
    private final Random random;

    public SeededRng(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }

    public long seed() {
        return seed;
    }

    @Override
    public int nextInt(int boundExclusive) {
        if (boundExclusive <= 0) {
            throw new IllegalArgumentException("bound must be > 0: " + boundExclusive);
        }
        return random.nextInt(boundExclusive);
    }

    @Override
    public int rangeInclusive(int minInclusive, int maxInclusive) {
        if (maxInclusive < minInclusive) {
            throw new IllegalArgumentException("max < min");
        }
        return minInclusive + random.nextInt(maxInclusive - minInclusive + 1);
    }

    @Override
    public double nextDouble() {
        return random.nextDouble();
    }

    @Override
    public boolean chance(double probability) {
        if (probability <= 0.0) {
            return false;
        }
        if (probability >= 1.0) {
            return true;
        }
        return random.nextDouble() < probability;
    }

    @Override
    public int rollPercent0to200() {
        return random.nextInt(201);
    }

    @Override
    public Rng fork(long salt) {
        return new SeededRng(seed * 0x9E3779B97F4A7C15L + salt);
    }
}
