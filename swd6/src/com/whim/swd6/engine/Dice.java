package com.whim.swd6.engine;

import java.util.Random;

/**
 * A thin, seedable wrapper over {@link java.util.Random} providing six-sided die
 * rolls. All randomness in the engine flows through a single {@code Dice} instance
 * so tests can inject a fixed-seed {@link Random} for deterministic behaviour.
 *
 * Owned by Task 2 (engine).
 */
public final class Dice {

    private final Random random;

    /** Default constructor: an unseeded {@link Random}. */
    public Dice() {
        this(new Random());
    }

    /** Seedable constructor: inject a {@link Random} for deterministic tests. */
    public Dice(Random random) {
        this.random = random == null ? new Random() : random;
    }

    /** Roll a single six-sided die, returning an integer in the inclusive range 1..6. */
    public int rollDie() {
        return random.nextInt(6) + 1;
    }
}
