package com.whim.bc3k.sim;

import com.whim.bc3k.api.Enums;
import com.whim.bc3k.sim.ship.ShipSystems;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShipSystemsTest {

    @Test public void allocationRespectsPerSystemCap() {
        ShipSystems s = new ShipSystems();
        boolean rejected = false;
        for (int i = 0; i < 20; i++) if (!s.allocate(Enums.PowerSystem.SHIELDS, 5)) { rejected = true; break; }
        assertTrue(rejected);
        assertTrue(s.system(Enums.PowerSystem.SHIELDS).power() <= ShipSystems.MAX_PER_SYSTEM);
    }

    @Test public void allocationRespectsReactorBudget() {
        ShipSystems s = new ShipSystems();
        for (Enums.PowerSystem sys : Enums.PowerSystem.values())
            for (int i = 0; i < 20; i++) s.allocate(sys, 5);
        assertTrue(s.reactorUsed() <= s.reactorOutput());
    }

    @Test public void offlineReactorBlocksAllocationUntilRestart() {
        ShipSystems s = new ShipSystems();
        s.shutdownReactor();
        assertFalse(s.allocate(Enums.PowerSystem.WEAPONS, 5));
        assertEquals(0, s.reactorOutput());
        assertTrue(s.restartReactor());
        assertTrue(s.allocate(Enums.PowerSystem.WEAPONS, 5));
    }

    @Test public void shieldsAbsorbDamageBeforeHull() {
        ShipSystems s = new ShipSystems();
        int shields0 = s.shields();
        s.applyDamage(100, null);
        assertEquals(shields0 - 100, s.shields());
        assertEquals(s.maxHull(), s.hull());   // hull untouched while shields hold
    }

    @Test public void overkillDamageSpillsToHullAndDestroys() {
        ShipSystems s = new ShipSystems();
        s.applyDamage(100000, null);
        assertEquals(0, s.hull());
        assertTrue(s.destroyed());
    }

    @Test public void systemBreachesAtZeroIntegrityAndRepairClearsIt() {
        ShipSystems s = new ShipSystems();
        s.damageSystem(Enums.PowerSystem.ENGINES, 200);   // over-damage
        assertTrue(s.system(Enums.PowerSystem.ENGINES).breached());
        assertEquals(0.0, s.system(Enums.PowerSystem.ENGINES).effectiveness(), 1e-9);
        s.repairSystem(Enums.PowerSystem.ENGINES, 50);
        assertFalse(s.system(Enums.PowerSystem.ENGINES).breached());
    }

    @Test public void shieldsRegenWhilePoweredAndOnline() {
        ShipSystems s = new ShipSystems();
        s.applyDamage(200, null);            // drop shields
        int low = s.shields();
        for (int i = 0; i < 20; i++) s.tick(0.1);
        assertTrue(s.shields() > low);
        assertTrue(s.shields() <= s.maxShields());
    }
}
