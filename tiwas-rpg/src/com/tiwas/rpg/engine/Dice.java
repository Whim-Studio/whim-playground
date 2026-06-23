package com.tiwas.rpg.engine;

import java.util.Random;

/**
 * Simple percentile dice. {@link #d100()} returns a uniform integer in [1,100].
 * Deterministic constructors are provided for reproducible tests.
 */
public final class Dice {
    private final Random random;

    public Dice() {
        this.random = new Random();
    }

    public Dice(long seed) {
        this.random = new Random(seed);
    }

    public Dice(Random r) {
        if (r == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        this.random = r;
    }

    /** Uniform 1..100 inclusive. */
    public int d100() {
        return random.nextInt(100) + 1;
    }
}
