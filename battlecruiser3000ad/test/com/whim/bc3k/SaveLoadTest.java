package com.whim.bc3k;

import com.whim.bc3k.api.Enums;
import com.whim.bc3k.engine.Engine;
import com.whim.bc3k.save.SaveManager;

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SaveLoadTest {

    @Test public void snapshotRestoreRoundTripPreservesState() {
        Engine a = new Engine();
        a.newGame(Enums.GameMode.CAMPAIGN, "Round Trip");
        a.jumpTo(1);                                   // move + burn fuel
        a.setPower(Enums.PowerSystem.WEAPONS, 5);
        a.resolveObjective();
        Properties snap = a.snapshot();

        Engine b = new Engine();
        b.restore(snap);

        assertTrue(b.view().started());
        assertEquals(a.view().gameMode(), b.view().gameMode());
        assertEquals(a.view().ship().name(), b.view().ship().name());
        assertEquals(a.view().cargo().fuel(), b.view().cargo().fuel());
        assertEquals(a.view().ship().power(Enums.PowerSystem.WEAPONS),
                     b.view().ship().power(Enums.PowerSystem.WEAPONS));
        assertEquals(a.view().galaxy().currentId(), b.view().galaxy().currentId());
        assertEquals(a.view().campaign().resolved(), b.view().campaign().resolved());
        assertEquals(a.view().crew().size(), b.view().crew().size());
    }

    @Test public void restoringXtremeReArmsCombat() {
        Engine a = new Engine();
        a.newGame(Enums.GameMode.XTREME_CARNAGE, "X");
        Properties snap = a.snapshot();

        Engine b = new Engine();
        b.restore(snap);
        assertEquals(Enums.GameMode.XTREME_CARNAGE, b.view().gameMode());
        assertNotNull(b.view().combat());
    }

    @Test public void freeFlightHasNoCampaignAfterRestore() {
        Engine a = new Engine();
        a.newGame(Enums.GameMode.FREE_FLIGHT, "FF");
        Engine b = new Engine();
        b.restore(a.snapshot());
        assertNull(b.view().campaign());
    }

    @Test public void saveManagerWritesReadsAndMissesGracefully() {
        SaveManager sm = new SaveManager("target/test-saves");
        Properties p = new Properties();
        p.setProperty("k", "v");
        assertTrue(sm.write("slotX", p));
        assertTrue(sm.exists("slotX"));
        assertEquals("v", sm.read("slotX").getProperty("k"));
        assertNull(sm.read("does-not-exist"));
    }

    @Test public void loadFailsForUnknownSlot() {
        Engine e = new Engine();
        assertTrue(!e.load("no-such-slot-xyz").isSuccess());
    }
}
