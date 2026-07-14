package com.whim.necromunda.engine;

import java.util.Random;

/**
 * A seedable dice wrapper. All randomness in the engine flows through one
 * {@code Dice} instance so a battle is fully deterministic given its seed —
 * essential for replayable saves and reproducible tests.
 */
public final class Dice {

    private final Random rng;

    public Dice(long seed) {
        this.rng = new Random(seed);
    }

    /** A single D6, 1..6. */
    public int d6() {
        return rng.nextInt(6) + 1;
    }

    /** A D3, 1..3, via the standard (D6+1)/2 mapping (1-2->1, 3-4->2, 5-6->3). */
    public int d3() {
        return (d6() + 1) / 2;
    }

    /** Sum of two D6, 2..12 (used for Leadership / bottle / nerve tests). */
    public int roll2d6() {
        return d6() + d6();
    }

    /** Sum of n D6. */
    public int rollN(int n) {
        int total = 0;
        for (int i = 0; i < n; i++) {
            total += d6();
        }
        return total;
    }
}
