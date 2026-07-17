package com.whim.xcom.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.whim.xcom.rules.model.ReactionModel;

/** Proves reaction score = reactions × currentTU / maxTU and the trigger rule. */
public class ReactionModelTest {

    private final ReactionModel rx = Ruleset1994.load().reactions();

    @Test
    public void scoreScalesWithRemainingTu() {
        assertEquals(63.0, rx.reactionScore(63, 54, 54), 1e-9); // full TU
        assertEquals(31.5, rx.reactionScore(63, 27, 54), 1e-9); // half TU
        assertEquals(0.0, rx.reactionScore(63, 0, 54), 1e-9);   // spent
    }

    @Test
    public void higherScoreInterruptsLower() {
        // Reactor full TU (score 63) vs mover full TU (score 50) -> reactor fires.
        assertTrue(rx.triggers(63, 54, 54, 50, 54, 54));
    }

    @Test
    public void equalScoresDoNotTrigger() {
        assertFalse(rx.triggers(50, 54, 54, 50, 54, 54));
    }

    @Test
    public void spentReactorCannotInterrupt() {
        assertFalse(rx.triggers(63, 0, 54, 50, 54, 54));
    }
}
