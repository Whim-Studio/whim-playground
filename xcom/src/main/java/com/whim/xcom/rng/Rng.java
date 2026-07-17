package com.whim.xcom.rng;

/**
 * Deterministic, injectable random-number source used by ALL rules.
 *
 * <p>The rules layer must never call {@link java.lang.Math#random()} or
 * {@code new java.util.Random()} directly — everything routes through an
 * {@code Rng} so that a fixed seed produces a fully reproducible game
 * (replays, unit tests, networked lockstep later).</p>
 */
public interface Rng {

    /** @return a uniform int in {@code [0, boundExclusive)}. */
    int nextInt(int boundExclusive);

    /** @return a uniform int in {@code [minInclusive, maxInclusive]}. */
    int rangeInclusive(int minInclusive, int maxInclusive);

    /** @return a uniform double in {@code [0.0, 1.0)}. */
    double nextDouble();

    /** @return {@code true} with the given probability (0..1). */
    boolean chance(double probability);

    /**
     * X-COM's ubiquitous "0%..200% of nominal" roll (mean = 100%).
     * Damage, several checks and score jitter all use this shape.
     *
     * @return a uniform int in {@code [0, 200]}.
     */
    int rollPercent0to200();

    /** @return a fresh independent stream seeded from this one (for sub-systems). */
    Rng fork(long salt);
}
