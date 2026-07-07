package com.whim.b5wars.model;

/** Dice specification for a weapon's damage: {@code count} d{@code sides} + {@code plus}. */
public final class DamageProfile {
    private final int count;
    private final int sides;
    private final int plus;

    public DamageProfile(int count, int sides, int plus) {
        this.count = count;
        this.sides = sides;
        this.plus = plus;
    }

    public int getCount() {
        return count;
    }

    public int getSides() {
        return sides;
    }

    public int getPlus() {
        return plus;
    }

    /** Roll this damage profile using the supplied Dice. */
    public int roll(Dice dice) {
        return dice.roll(count, sides, plus);
    }
}
