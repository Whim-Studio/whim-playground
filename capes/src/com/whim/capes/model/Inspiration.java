package com.whim.capes.model;

/**
 * An Inspiration won by Resolving a Conflict (pp.30-31). Its {@link #value} is
 * the die difference (or full value of an unpaired die) computed at Resolve.
 * It is tagged {@link #forWhom} to a particular character or group ("For the
 * villains", "For Liz Livingstone"), and held by a {@link Player}.
 *
 * <p>Spending an Inspiration raises one die on that character's side to the
 * Inspiration's value (p.25). Its value may itself be raised one point at a
 * time by using an Ability whose score &ge; the current value (p.38).
 */
public final class Inspiration implements java.io.Serializable {
    private int value;
    private final String forWhom; // character id or free-text group label

    public Inspiration(int value, String forWhom) {
        this.value = value;
        this.forWhom = forWhom;
    }

    public int value() { return value; }
    public String forWhom() { return forWhom; }

    /** Raise the Inspiration by one point (p.38). Capped at the die maximum of 6. */
    public void raiseByOne() {
        if (value < Die.MAX) value++;
    }

    @Override public String toString() { return "Inspiration " + value + " for " + forWhom; }
}
