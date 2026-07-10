package com.whim.scg;

import com.whim.scg.api.ActionResult;
import com.whim.scg.api.Enums;
import com.whim.scg.api.Views;
import com.whim.scg.engine.Engine;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Smoke tests for combat damage and save round-trip. */
public class EngineTest {

    private Engine fresh() {
        Engine e = new Engine();
        e.newGame("Ada", "Wildstar");
        return e;
    }

    @Test
    public void newGameBuildsShipCrewGalaxy() {
        Engine e = fresh();
        Views.GameView v = e.view();
        assertNotNull("player ship exists", v.playerShip());
        assertEquals("Wildstar", v.playerShip().name());
        assertTrue("starting crew ~4", v.playerShip().crew().size() >= 3);
        assertEquals(150, v.credits());
        assertEquals(1, v.day());
        assertNotNull("galaxy exists", v.galaxy());
        int n = v.galaxy().systems().size();
        assertTrue("10-14 systems, got " + n, n >= 10 && n <= 14);
        assertFalse("weapons loaded", v.playerShip().weapons().isEmpty());
    }

    @Test
    public void combatDamagesEnemyOverTime() {
        Engine e = fresh();
        e.setMode(Enums.Mode.SPACE_COMBAT); // engine stages a skirmish
        Views.CombatView cb = e.view().combat();
        assertNotNull("combat staged", cb);
        int startHull = cb.enemy().hull();
        // simulate ~30 seconds of combat at 60fps
        for (int i = 0; i < 1800 && !e.view().combat().over() && e.view().mode() == Enums.Mode.SPACE_COMBAT; i++) {
            e.tick(1.0 / 60.0);
        }
        // either the enemy took damage or the fight already resolved to a player win
        boolean resolvedWin = e.view().mode() == Enums.Mode.GALAXY_MAP || e.view().mode() == Enums.Mode.VICTORY;
        boolean damaged = resolvedWin || e.view().combat() == null || e.view().combat().enemy().hull() < startHull;
        assertTrue("enemy hull should drop below " + startHull, damaged);
    }

    @Test
    public void pausePreventsSimulation() {
        Engine e = fresh();
        e.setMode(Enums.Mode.SPACE_COMBAT);
        e.togglePause();
        assertTrue(e.view().paused());
        int hull = e.view().combat().enemy().hull();
        for (int i = 0; i < 600; i++) e.tick(1.0 / 60.0);
        assertEquals("paused: no damage", hull, e.view().combat().enemy().hull());
    }

    @Test
    public void intentsNeverThrowWhenEmpty() {
        Engine e = new Engine(); // no newGame
        // none of these should throw; all should fail gracefully
        assertFalse(e.assignCrew(1, 1).isSuccess());
        assertFalse(e.jumpTo(3).isSuccess());
        assertFalse(e.fireWeapon(0).isSuccess());
        assertFalse(e.beginBoarding().isSuccess());
        assertFalse(e.buyTech(Enums.TechType.HULL).isSuccess());
        e.tick(0.016); // must not throw
    }

    @Test
    public void saveRoundTripPreservesState() {
        Engine e = fresh();
        // mutate some state
        int roomId = e.view().playerShip().rooms().get(0).id();
        e.renameCrew(e.view().playerShip().crew().get(0).id(), "Renamed");
        String slot = "junit_roundtrip";
        ActionResult saved = e.save(slot);
        assertTrue(saved.message(), saved.isSuccess());

        int credits = e.view().credits();
        int hull = e.view().playerShip().hull();
        String captainShip = e.view().playerShip().name();
        int crewCount = e.view().playerShip().crew().size();
        int systems = e.view().galaxy().systems().size();

        // change live state, then load should overwrite it
        e.newGame("Zed", "Other");
        assertEquals("Other", e.view().playerShip().name());

        ActionResult loaded = e.load(slot);
        assertTrue(loaded.message(), loaded.isSuccess());
        assertEquals(credits, e.view().credits());
        assertEquals(hull, e.view().playerShip().hull());
        assertEquals(captainShip, e.view().playerShip().name());
        assertEquals(crewCount, e.view().playerShip().crew().size());
        assertEquals(systems, e.view().galaxy().systems().size());
        assertEquals(roomId, e.view().playerShip().rooms().get(0).id());
    }
}
