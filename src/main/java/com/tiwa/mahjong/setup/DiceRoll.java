package com.tiwa.mahjong.setup;

import java.util.Random;

/**
 * A roll of three six-sided dice (total 3-18), as used to determine seating and the wall break.
 * Deterministic given the supplied {@link Random}.
 */
public final class DiceRoll {

    private final int d1;
    private final int d2;
    private final int d3;

    public DiceRoll(int d1, int d2, int d3) {
        this.d1 = checkDie(d1);
        this.d2 = checkDie(d2);
        this.d3 = checkDie(d3);
    }

    private static int checkDie(int d) {
        if (d < 1 || d > 6) {
            throw new IllegalArgumentException("die must be 1..6, got " + d);
        }
        return d;
    }

    /** Rolls three dice from the supplied source of randomness. */
    public static DiceRoll roll(Random random) {
        return new DiceRoll(1 + random.nextInt(6), 1 + random.nextInt(6), 1 + random.nextInt(6));
    }

    public int getDie1() {
        return d1;
    }

    public int getDie2() {
        return d2;
    }

    public int getDie3() {
        return d3;
    }

    /** Sum of the three dice (3-18). */
    public int total() {
        return d1 + d2 + d3;
    }

    @Override
    public String toString() {
        return "DiceRoll(" + d1 + "," + d2 + "," + d3 + "=" + total() + ")";
    }
}
