package com.tycoon.core;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AutoTurnEngineTest {

    /** A stub processor: emits an interrupt at a configured hour, otherwise none.
     *  Also asserts that the FloorPlan is locked while processing. */
    private static final class StubProcessor implements TurnProcessor {
        final long interruptAtHour; // -1 = never
        boolean sawLockedDuringProcessing = true;

        StubProcessor(long interruptAtHour) {
            this.interruptAtHour = interruptAtHour;
        }

        @Override
        public List<Interrupt> processHour(GameState state) {
            if (!state.player().floorPlan().isLocked()) {
                sawLockedDuringProcessing = false;
            }
            List<Interrupt> out = new ArrayList<Interrupt>();
            if (state.hour() == interruptAtHour) {
                out.add(new Interrupt(InterruptType.DEVELOPMENT_MILESTONE, state.hour(), "milestone"));
            }
            return out;
        }
    }

    @Test
    public void haltsOnFirstInterrupt() {
        GameState state = GameState.newGame(1L);
        StubProcessor proc = new StubProcessor(3L); // interrupt when processing hour 3
        AutoTurnEngine engine = new AutoTurnEngine(state, proc);

        List<Interrupt> result = engine.run(100);

        assertEquals(1, result.size());
        assertEquals(InterruptType.DEVELOPMENT_MILESTONE, result.get(0).type());
        assertEquals(3L, result.get(0).hour());
        // Hours 0,1,2,3 processed -> counter advanced to 4.
        assertEquals(4L, state.hour());
        // FloorPlan unlocked on return.
        assertFalse(state.player().floorPlan().isLocked());
        assertTrue("plan should be locked during processing", proc.sawLockedDuringProcessing);
    }

    @Test
    public void budgetExhaustedReturnsManualPause() {
        GameState state = GameState.newGame(1L);
        StubProcessor proc = new StubProcessor(-1L); // never interrupts
        AutoTurnEngine engine = new AutoTurnEngine(state, proc);

        List<Interrupt> result = engine.run(10);

        assertEquals(1, result.size());
        assertEquals(InterruptType.MANUAL_PAUSE, result.get(0).type());
        assertEquals(10L, state.hour());
        assertFalse(state.player().floorPlan().isLocked());
    }

    @Test
    public void stepAdvancesExactlyOneHour() {
        GameState state = GameState.newGame(1L);
        StubProcessor proc = new StubProcessor(0L); // would interrupt at hour 0
        AutoTurnEngine engine = new AutoTurnEngine(state, proc);

        List<Interrupt> result = engine.step();
        assertEquals(1L, state.hour());
        assertEquals(1, result.size());
        assertEquals(InterruptType.DEVELOPMENT_MILESTONE, result.get(0).type());
        assertFalse(state.player().floorPlan().isLocked());

        // Next step at hour 1 -> no interrupt configured.
        List<Interrupt> result2 = engine.step();
        assertEquals(2L, state.hour());
        assertTrue(result2.isEmpty());
    }
}
