package com.whim.b5db.engine;

import java.util.List;
import java.util.Random;

/**
 * Deterministic random source. All shuffles and AI coin-flips route through a
 * single seeded {@link Random} so a given seed reproduces an entire game or
 * simulation batch exactly (a hard requirement for the Monte-Carlo harness).
 */
public final class Rng {

    private final Random random;
    private final long seed;

    public Rng(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }

    public long seed() {
        return seed;
    }

    public int nextInt(int bound) {
        return random.nextInt(bound);
    }

    public boolean nextBoolean() {
        return random.nextBoolean();
    }

    /** Fisher-Yates shuffle driven by this deterministic source. */
    public <T> void shuffle(List<T> list) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            T tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }
    }
}
