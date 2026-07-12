package com.whim.capes.engine;

import java.util.Random;

/**
 * Injectable source of d6 rolls, so the engine's randomness is deterministic in
 * tests (seeded) and pluggable in the UI. Rolling is centralized here rather
 * than in {@link com.whim.capes.model.Die} to keep the model pure.
 */
public interface Roller {
    /** Returns a value in 1..6. */
    int rollD6();

    /** Default RNG-backed roller; seedable for reproducible tests. */
    final class SeededRoller implements Roller {
        private final Random rng;
        public SeededRoller() { this.rng = new Random(); }
        public SeededRoller(long seed) { this.rng = new Random(seed); }
        @Override public int rollD6() { return 1 + rng.nextInt(6); }
    }

    /** Fixed-sequence roller for tests: returns the given values in order, then repeats the last. */
    final class ScriptedRoller implements Roller {
        private final int[] values;
        private int i = 0;
        public ScriptedRoller(int... values) { this.values = values; }
        @Override public int rollD6() {
            int v = values[Math.min(i, values.length - 1)];
            i++;
            return Math.max(1, Math.min(6, v));
        }
    }
}
