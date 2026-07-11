package com.whim.bc3k.sim;

import com.whim.bc3k.sim.campaign.Campaign;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class CampaignTest {

    @Test public void threatRisesOverTime() {
        Campaign c = new Campaign();
        int t0 = c.threat();
        for (int i = 0; i < 100; i++) c.tick(0.5);   // 50s
        assertTrue(c.threat() > t0);
    }

    @Test public void threatIsCappedAt100() {
        Campaign c = new Campaign();
        for (int i = 0; i < 10000; i++) c.tick(1.0);
        assertTrue(c.threat() <= 100);
        assertTrue(c.critical());
    }

    @Test public void resolvingObjectiveAdvancesAndRelievesThreat() {
        Campaign c = new Campaign();
        for (int i = 0; i < 100; i++) c.tick(0.5);   // build some threat
        int before = c.threat();
        String obj0 = c.objective();
        c.resolveObjective();
        assertTrue(c.threat() < before);
        assertEquals(1, c.resolvedCount());
        assertNotEquals(obj0, c.objective());        // rotated to the next objective
    }

    @Test public void threatNeverGoesNegative() {
        Campaign c = new Campaign();
        for (int i = 0; i < 10; i++) c.resolveObjective();
        assertTrue(c.threat() >= 0);
    }
}
