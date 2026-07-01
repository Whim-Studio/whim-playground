package com.whim.colony.domain;

/**
 * Mutable per-colonist need levels, each a double in the range [0..100] where
 * 100 is fully satisfied and 0 is critical. Setters clamp to the valid range.
 * The engine (Task 2) decays and replenishes these; the UI (Task 3) reads them.
 */
public final class Needs {
    public static final double MIN = 0.0;
    public static final double MAX = 100.0;

    /** Below this a colonist is considered hungry / tired / unhappy. */
    public static final double LOW_THRESHOLD = 25.0;
    /** Below this the condition is critical (starving / exhausted / breaking). */
    public static final double CRITICAL_THRESHOLD = 10.0;

    private double hunger;
    private double rest;
    private double mood;

    public Needs() {
        this(MAX, MAX, MAX);
    }

    public Needs(double hunger, double rest, double mood) {
        setHunger(hunger);
        setRest(rest);
        setMood(mood);
    }

    public double getHunger() {
        return hunger;
    }

    public void setHunger(double hunger) {
        this.hunger = clamp(hunger);
    }

    public double getRest() {
        return rest;
    }

    public void setRest(double rest) {
        this.rest = clamp(rest);
    }

    public double getMood() {
        return mood;
    }

    public void setMood(double mood) {
        this.mood = clamp(mood);
    }

    /** Clamp a value into the [MIN..MAX] range. */
    public static double clamp(double value) {
        if (value < MIN) {
            return MIN;
        }
        if (value > MAX) {
            return MAX;
        }
        return value;
    }
}
