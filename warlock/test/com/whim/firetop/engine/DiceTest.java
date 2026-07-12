package com.whim.firetop.engine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Verifies the dice roller and the FF attribute-generation formulas. */
public class DiceTest {

    @Test
    public void rollSingleWithinBounds() {
        Dice d = new Dice(1L);
        for (int i = 0; i < 2000; i++) {
            int r = d.roll(6);
            assertTrue("roll in 1..6", r >= 1 && r <= 6);
        }
    }

    @Test
    public void seededReproducible() {
        Dice a = new Dice(42L);
        Dice b = new Dice(42L);
        for (int i = 0; i < 100; i++) {
            assertEquals("same seed => same stream", a.roll(2, 6), b.roll(2, 6));
        }
    }

    @Test
    public void d6Range() {
        Dice d = new Dice(7L);
        for (int i = 0; i < 2000; i++) {
            int r = d.d6();
            assertTrue(r >= 1 && r <= 6);
        }
    }

    @Test
    public void roll2d6Range() {
        Dice d = new Dice(9L);
        for (int i = 0; i < 5000; i++) {
            int r = d.roll2d6();
            assertTrue("2d6 in 2..12", r >= 2 && r <= 12);
        }
    }

    @Test
    public void rollSkillRange() {
        Dice d = new Dice(11L);
        for (int i = 0; i < 5000; i++) {
            int r = d.rollSkill();
            assertTrue("SKILL 7..12", r >= 7 && r <= 12);
        }
    }

    @Test
    public void rollStaminaRange() {
        Dice d = new Dice(13L);
        for (int i = 0; i < 5000; i++) {
            int r = d.rollStamina();
            assertTrue("STAMINA 14..24", r >= 14 && r <= 24);
        }
    }

    @Test
    public void rollLuckRange() {
        Dice d = new Dice(17L);
        for (int i = 0; i < 5000; i++) {
            int r = d.rollLuck();
            assertTrue("LUCK 7..12", r >= 7 && r <= 12);
        }
    }
}
