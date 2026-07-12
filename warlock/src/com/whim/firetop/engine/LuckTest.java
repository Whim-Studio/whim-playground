package com.whim.firetop.engine;

import com.whim.firetop.model.Character;

/**
 * Implements "Test your Luck": roll 2d6; the character is Lucky if the roll is at
 * most their current LUCK, and every test spends one point of LUCK.
 */
public final class LuckTest {

    private LuckTest() { }

    /** Immutable outcome of a Luck test. */
    public static final class Result {
        private final boolean lucky;
        private final int roll;
        private final int luckBefore;

        public Result(boolean lucky, int roll, int luckBefore) {
            this.lucky = lucky;
            this.roll = roll;
            this.luckBefore = luckBefore;
        }

        public boolean isLucky() { return lucky; }
        public int getRoll() { return roll; }
        public int getLuckBefore() { return luckBefore; }
    }

    /**
     * Tests the character's luck: rolls 2d6, compares to current LUCK, then spends
     * one LUCK. Lucky when {@code roll <= luckBefore}.
     */
    public static Result test(Character c, Dice dice) {
        int luckBefore = c.getLuckCurrent();
        int roll = dice.roll2d6();
        boolean lucky = roll <= luckBefore;
        c.spendLuck();
        return new Result(lucky, roll, luckBefore);
    }
}
