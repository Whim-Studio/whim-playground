package com.whim.firetop.engine;

import java.io.Serializable;
import java.util.Random;

/**
 * Dice roller for the game. Backed by {@link java.util.Random}; construct with a
 * seed for deterministic tests and reproducible games, or without for a real
 * random session. All Fighting Fantasy attribute-generation formulas are exposed
 * here so they live in exactly one place.
 */
public final class Dice implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Random random;

    /** Real-random dice (system-seeded). */
    public Dice() {
        this.random = new Random();
    }

    /** Deterministic dice for tests / reproducible play. */
    public Dice(long seed) {
        this.random = new Random(seed);
    }

    /** Rolls a single die with the given number of sides (1..sides). */
    public int roll(int sides) {
        if (sides < 1) {
            throw new IllegalArgumentException("sides must be >= 1");
        }
        return random.nextInt(sides) + 1;
    }

    /** Rolls {@code n} dice of {@code sides} and sums them (nDm). */
    public int roll(int n, int sides) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be >= 0");
        }
        int total = 0;
        for (int i = 0; i < n; i++) {
            total += roll(sides);
        }
        return total;
    }

    /** A single six-sided die. */
    public int d6() {
        return roll(6);
    }

    /** Two six-sided dice summed (2..12) — the core FF roll. */
    public int roll2d6() {
        return roll(2, 6);
    }

    /** SKILL = 1d6 + 6 (range 7..12). */
    public int rollSkill() {
        return roll(6) + 6;
    }

    /** STAMINA = 2d6 + 12 (range 14..24). */
    public int rollStamina() {
        return roll(2, 6) + 12;
    }

    /** LUCK = 1d6 + 6 (range 7..12). */
    public int rollLuck() {
        return roll(6) + 6;
    }
}
