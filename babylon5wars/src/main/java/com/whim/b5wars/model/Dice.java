package com.whim.b5wars.model;

import java.util.Random;

/** Seedable RNG — the single source of randomness so games are reproducible. */
public final class Dice {
    private final Random rng;

    public Dice(long seed) {
        this.rng = new Random(seed);
    }

    /** Roll a single die: result in 1..sides. */
    public int d(int sides) {
        if (sides < 1) {
            throw new IllegalArgumentException("sides must be >= 1: " + sides);
        }
        return rng.nextInt(sides) + 1;
    }

    /** Roll a d20: result in 1..20. */
    public int d20() {
        return d(20);
    }

    /** Sum of {@code count} dice of {@code sides} sides, plus a flat {@code plus} modifier. */
    public int roll(int count, int sides, int plus) {
        int total = plus;
        for (int i = 0; i < count; i++) {
            total += d(sides);
        }
        return total;
    }
}
