package com.whim.bc3k;

import com.whim.bc3k.api.Enums;
import com.whim.bc3k.engine.Engine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Engine-level tests for the promoted fighter and ground-combat capabilities. */
public class CombatFeaturesTest {

    @Test public void launchingAFighterInCombatCommitsItToTheDogfight() {
        Engine e = new Engine();
        e.newGame(Enums.GameMode.XTREME_CARNAGE, "X");
        assertEquals(0, e.view().combat().playerFighters());
        assertTrue(e.launchCraft(Enums.CraftType.FIGHTER).isSuccess());
        assertEquals(1, e.view().combat().playerFighters());
        assertTrue(e.recallCraft(Enums.CraftType.FIGHTER).isSuccess());
        assertEquals(0, e.view().combat().playerFighters());
    }

    @Test public void deployAtvStartsGroundSkirmishAndSwitchesScreen() {
        Engine e = new Engine();
        e.newGame(Enums.GameMode.FREE_FLIGHT, "G");
        assertTrue(e.deployAtv().isSuccess());
        assertNotNull(e.view().ground());
        assertEquals(Enums.Mode.GROUND, e.view().mode());
        assertTrue(e.assaultGround().isSuccess());
    }

    @Test public void assaultFailsWithoutAnActiveSkirmish() {
        Engine e = new Engine();
        e.newGame(Enums.GameMode.FREE_FLIGHT, "G");
        assertFalse(e.assaultGround().isSuccess());
    }

    @Test public void groundSkirmishResolvesUnderTick() {
        Engine e = new Engine();
        e.newGame(Enums.GameMode.FREE_FLIGHT, "G");
        e.deployAtv();
        for (int i = 0; i < 4000 && !e.view().ground().over(); i++) e.tick(0.5);
        assertTrue(e.view().ground().over());
    }
}
