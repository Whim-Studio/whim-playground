package com.spacemines;

import java.util.Random;

/**
 * Wraps a {@link java.util.Random} and replicates the legacy BASIC idiom
 * {@code INT(RND(1)*range+base)} used throughout the original "Space Mines".
 *
 * In Commodore BASIC, {@code INT(RND(1)*range+base)} yields an integer in the
 * range {@code [base, base+range-1]}. {@code rng.nextInt(range) + base}
 * produces exactly that same inclusive range, so it is a faithful port.
 */
public class RandomEvents {

    private final Random rng;

    public RandomEvents(Random rng) {
        this.rng = rng;
    }

    /**
     * Faithful port of BASIC {@code INT(RND(1)*range+base)}.
     *
     * @param range size of the random span (must be &gt; 0)
     * @param base  additive offset / lower bound
     * @return an int in {@code [base, base+range-1]}
     */
    public int rnd(int range, int base) {
        // BASIC: INT(RND(1)*range+base)
        return rng.nextInt(range) + base;
    }

    /**
     * Next year's food price.
     * BASIC: FP = INT(RND(1)*10+5)  -> price in [5, 14]
     */
    public int nextFoodPrice() {
        // BASIC: FP=INT(RND(1)*10+5)
        return rnd(10, 5);
    }

    /**
     * Next year's ore produced per mine.
     * BASIC: CE = INT(RND(1)*10+10) -> output in [10, 19]
     */
    public int nextOrePerMine() {
        // BASIC: CE=INT(RND(1)*10+10)
        return rnd(10, 10);
    }
}
