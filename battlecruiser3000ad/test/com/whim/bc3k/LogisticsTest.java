package com.whim.bc3k;

import com.whim.bc3k.api.Enums;
import com.whim.bc3k.engine.Engine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Phase 5 logistics/comms/flight-deck intents, exercised headlessly through the engine. */
public class LogisticsTest {

    private Engine game() {
        Engine e = new Engine();
        e.newGame(Enums.GameMode.FREE_FLIGHT, "Test Cruiser");
        return e;
    }

    @Test public void jumpConsumesFuelAndRefuelRestores() {
        Engine e = game();
        int fuel0 = e.view().cargo().fuel();
        assertTrue(e.jumpTo(1).isSuccess());               // Sol -> Centauri (has station)
        assertTrue(e.view().cargo().fuel() < fuel0);
        assertTrue(e.refuel().isSuccess());
        assertEquals(e.view().cargo().maxFuel(), e.view().cargo().fuel());
    }

    @Test public void refuelFailsWithoutStation() {
        Engine e = game();
        assertTrue(e.jumpTo(2).isSuccess());               // Sol -> Sirius (no station)
        assertFalse(e.refuel().isSuccess());
    }

    @Test public void launchAndRecallCraftTracksCounts() {
        Engine e = game();
        Enums.CraftType f = Enums.CraftType.FIGHTER;
        assertTrue(e.launchCraft(f).isSuccess());
        assertEquals(1, e.view().craft().get(0).launched());
        assertTrue(e.recallCraft(f).isSuccess());
        assertEquals(0, e.view().craft().get(0).launched());
    }

    @Test public void cannotLaunchMoreCraftThanCarried() {
        Engine e = game();
        Enums.CraftType f = Enums.CraftType.FIGHTER;
        int total = e.view().craft().get(0).total();
        for (int i = 0; i < total; i++) assertTrue(e.launchCraft(f).isSuccess());
        assertFalse(e.launchCraft(f).isSuccess());          // none left docked
    }

    @Test public void hailAlwaysOpensAChannel() {
        Engine e = game();
        assertTrue(e.hail().isSuccess());
    }

    @Test public void flashClearsAfterTtl() {
        Engine e = game();
        e.hail();
        assertFalse(e.view().flash().isEmpty());
        for (int i = 0; i < 100; i++) e.tick(0.1);          // 10s > 4s TTL
        assertTrue(e.view().flash().isEmpty());
    }
}
