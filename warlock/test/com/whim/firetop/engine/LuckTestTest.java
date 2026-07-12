package com.whim.firetop.engine;

import com.whim.firetop.model.Character;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Verifies Test-Your-Luck resolution and luck depletion. */
public class LuckTestTest {

    @Test
    public void luckSpentEachTest() {
        Character c = new Character("Hero", 10, 20, 10);
        Dice d = new Dice(3L);
        int before = c.getLuckCurrent();
        LuckTest.test(c, d);
        assertEquals("luck drops by 1 per test", before - 1, c.getLuckCurrent());
        LuckTest.test(c, d);
        assertEquals(before - 2, c.getLuckCurrent());
    }

    @Test
    public void luckyWhenRollAtMostLuck() {
        // LUCK 12: any 2d6 (max 12) is <= luck => always Lucky.
        Character c = new Character("Hero", 10, 20, 12);
        Dice d = new Dice(5L);
        LuckTest.Result r = LuckTest.test(c, d);
        assertTrue("2d6 <= 12 is always lucky", r.isLucky());
        assertTrue(r.getRoll() >= 2 && r.getRoll() <= 12);
    }

    @Test
    public void unluckyWhenRollExceedsLuck() {
        // LUCK 0: any 2d6 (min 2) is > luck => always Unlucky.
        Character c = new Character("Hero", 10, 20, 0);
        Dice d = new Dice(6L);
        LuckTest.Result r = LuckTest.test(c, d);
        assertFalse("2d6 > 0 is always unlucky", r.isLucky());
    }

    @Test
    public void luckNeverBelowZero() {
        Character c = new Character("Hero", 10, 20, 1);
        Dice d = new Dice(8L);
        LuckTest.test(c, d);
        LuckTest.test(c, d);
        LuckTest.test(c, d);
        assertEquals("luck floors at 0", 0, c.getLuckCurrent());
    }
}
