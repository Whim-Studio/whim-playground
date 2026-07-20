package com.heroquest.logic;

import com.heroquest.model.CombatDie;

import java.util.Random;

/** Central random source for movement (2d6) and the custom combat dice. */
public final class Dice {
    private final Random rng;

    public Dice(Random rng) {
        this.rng = rng;
    }

    public Dice() {
        this(new Random());
    }

    /** Standard six-sided roll, 1..6. */
    public int d6() {
        return rng.nextInt(6) + 1;
    }

    /** Heroes roll two red dice for movement. */
    public int rollMovement() {
        return d6() + d6();
    }

    /** Rolls {@code count} combat dice and returns the raw faces. */
    public CombatDie[] rollCombat(int count) {
        CombatDie[] out = new CombatDie[Math.max(0, count)];
        for (int i = 0; i < out.length; i++) {
            out[i] = CombatDie.FACES[rng.nextInt(CombatDie.FACES.length)];
        }
        return out;
    }

    public int nextInt(int bound) {
        return rng.nextInt(bound);
    }
}
