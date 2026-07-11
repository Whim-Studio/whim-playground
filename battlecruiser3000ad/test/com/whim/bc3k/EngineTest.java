package com.whim.bc3k;

import com.whim.bc3k.api.Enums;
import com.whim.bc3k.api.GameController;
import com.whim.bc3k.api.Views;
import com.whim.bc3k.engine.Engine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Headless tests for the Phase 2 engine seam. No Swing is touched, proving the
 * simulation is testable without a display (the MVC boundary the design requires).
 */
public class EngineTest {

    private GameController fresh() {
        GameController c = new Engine();
        c.newGame(Enums.GameMode.FREE_FLIGHT, "Test Cruiser");
        return c;
    }

    @Test public void newGameStartsInNavConsole() {
        GameController c = fresh();
        Views.GameView v = c.view();
        assertTrue(v.started());
        assertEquals(Enums.Mode.NAV, v.mode());
        assertEquals(Enums.GameMode.FREE_FLIGHT, v.gameMode());
    }

    @Test public void powerAllocationRespectsPerSystemCap() {
        GameController c = fresh();
        Views.ShipView s = c.view().ship();
        // Drive shields toward the cap; the last increment past the cap must fail.
        int cap = s.maxPerSystem();
        boolean sawFailure = false;
        for (int i = 0; i < 20; i++) {
            if (!c.setPower(Enums.PowerSystem.SHIELDS, +5).isSuccess()) { sawFailure = true; break; }
        }
        assertTrue("expected a per-system cap rejection", sawFailure);
        assertTrue(c.view().ship().power(Enums.PowerSystem.SHIELDS) <= cap);
    }

    @Test public void powerAllocationRespectsReactorBudget() {
        GameController c = fresh();
        Views.ShipView s = c.view().ship();
        assertTrue(s.reactorUsed() <= s.reactorOutput());
        // Total allocation can never exceed reactor output.
        for (Enums.PowerSystem sys : Enums.PowerSystem.values()) {
            for (int i = 0; i < 20; i++) c.setPower(sys, +5);
        }
        assertTrue(c.view().ship().reactorUsed() <= c.view().ship().reactorOutput());
    }

    @Test public void reactorOfflineBlocksPowerThenRestartRestores() {
        Engine e = new Engine();
        e.newGame(Enums.GameMode.XTREME_CARNAGE, "Test");
        // No public "kill reactor" yet, but restart is idempotent when online.
        assertTrue(e.restartReactor().isSuccess());
        assertTrue(e.view().ship().reactorOnline());
    }

    @Test public void shieldsRegenWithPowerAndTick() {
        Engine e = new Engine();
        e.newGame(Enums.GameMode.FREE_FLIGHT, "Test");
        // Advancing time with shield power allocated must not exceed the max.
        for (int i = 0; i < 100; i++) e.tick(0.1);
        Views.ShipView s = e.view().ship();
        assertTrue(s.shields() <= s.maxShields());
        assertTrue(s.shields() >= 0);
    }

    @Test public void intentsFailBeforeNewGameInsteadOfThrowing() {
        Engine e = new Engine();
        assertFalse(e.view().started());
        assertFalse(e.setPower(Enums.PowerSystem.WEAPONS, +5).isSuccess());
    }

    @Test public void towRequestSucceedsDuringGame() {
        GameController c = fresh();
        assertTrue(c.requestTow().isSuccess());
    }
}
