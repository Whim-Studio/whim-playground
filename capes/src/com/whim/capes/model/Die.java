package com.whim.capes.model;

/**
 * A single six-sided die sitting on one side of a Conflict.
 * <p>Dice start at value 1 (p.26) and range 1-6. A die belongs to the
 * {@link ConflictSide} that owns it. This class deliberately holds no RNG:
 * rolling is performed by the engine so that randomness is injectable and
 * unit-testable. A Die simply remembers its current face and the value it
 * held immediately before the last roll (needed for the "accept the roll or
 * turn it back" rule, p.38).
 */
public final class Die implements java.io.Serializable {
    public static final int MIN = 1;
    public static final int MAX = 6;

    private int value;
    private int previousValue;

    public Die() { this(MIN); }

    public Die(int value) {
        this.value = clamp(value);
        this.previousValue = this.value;
    }

    public int value() { return value; }

    public int previousValue() { return previousValue; }

    /**
     * Records a new rolled face, remembering the prior value so the actor can
     * revert (p.38). Does not decide acceptance; that is an engine concern.
     */
    public void placeRoll(int rolled) {
        this.previousValue = this.value;
        this.value = clamp(rolled);
    }

    /** Reverts to the value held before the last {@link #placeRoll(int)} (turn the die back). */
    public void revert() {
        this.value = this.previousValue;
    }

    /** Directly sets the face (used by Splitting, Inspirations, Gloating), updating history. */
    public void set(int newValue) {
        this.previousValue = this.value;
        this.value = clamp(newValue);
    }

    public static int clamp(int v) {
        if (v < MIN) return MIN;
        if (v > MAX) return MAX;
        return v;
    }

    @Override public String toString() { return Integer.toString(value); }
}
