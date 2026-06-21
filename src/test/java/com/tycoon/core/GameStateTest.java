package com.tycoon.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GameStateTest {

    @Test
    public void hourDayWeekCounter() {
        GameState state = GameState.newGame(42L);
        assertEquals(0L, state.hour());
        assertEquals(0, state.day());
        assertEquals(0, state.week());

        for (int i = 0; i < 24; i++) {
            state.advanceHourCounter();
        }
        assertEquals(24L, state.hour());
        assertEquals(1, state.day());
        assertEquals(0, state.week());

        // Advance to hour 7*24 = 168 -> day 7 -> week 1.
        for (long h = state.hour(); h < 168L; h++) {
            state.advanceHourCounter();
        }
        assertEquals(168L, state.hour());
        assertEquals(7, state.day());
        assertEquals(1, state.week());
    }

    @Test
    public void newGameBuilds100Competitors() {
        GameState state = GameState.newGame(7L);
        assertEquals(100, state.competitors().size());
        // Strengths should vary across [0,1].
        double min = 2.0;
        double max = -1.0;
        for (AiStudio ai : state.competitors()) {
            assertTrue(ai.strength() >= 0.0 && ai.strength() <= 1.0);
            min = Math.min(min, ai.strength());
            max = Math.max(max, ai.strength());
        }
        assertTrue("strengths should vary", max - min > 0.3);
    }

    @Test
    public void newGameDefaultFloorPlanAtLeast40x30() {
        GameState state = GameState.newGame(1L);
        FloorPlan plan = state.player().floorPlan();
        assertTrue(plan.width() >= 40);
        assertTrue(plan.height() >= 30);
        assertTrue("starting employees", state.player().employees().size() > 0);
    }

    @Test
    public void seededRngIsDeterministic() {
        GameState a = GameState.newGame(123L);
        GameState b = GameState.newGame(123L);
        for (int i = 0; i < 50; i++) {
            assertEquals(a.rng().nextInt(1000), b.rng().nextInt(1000));
        }
        assertNotNull(a.rng());
    }
}
