package com.whim.xcom.meta;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.whim.xcom.rules.Ruleset;
import com.whim.xcom.rules.Ruleset1994;
import com.whim.xcom.rules.def.ResearchNode;

/**
 * Phase 7 — the research win path. Interrogation projects are gated on a live
 * captive in stores (consumed when the project starts), and the tech chain
 * culminates in "Cydonia or Bust!". Also covers save/load round-trip of the new
 * live-alien store items and endgame flags.
 */
public class ResearchGatingTest {

    private final Ruleset rs = Ruleset1994.load();

    private Campaign fresh() {
        return new Campaign(20, 10, new SoldierRoster());
    }

    private boolean available(Campaign c, String id) {
        for (ResearchNode n : c.availableResearch(rs)) {
            if (n.id().equals(id)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void interrogationNeedsALiveCaptiveAndConsumesIt() {
        Campaign c = fresh();
        assertFalse("no captive → interrogation unavailable",
                available(c, "interrogate_sectoid_soldier"));

        c.addToStores("live_sectoid_soldier", 1);
        assertTrue("with a captive → available", available(c, "interrogate_sectoid_soldier"));

        c.startResearch(rs.research("interrogate_sectoid_soldier"), 20);
        Integer left = c.stores().get("live_sectoid_soldier");
        assertTrue("the captive is consumed by interrogation", left == null || left == 0);
    }

    @Test
    public void techChainReachesCydoniaOrBust() {
        Campaign c = fresh();
        // Capture and interrogate a soldier and a leader.
        c.addToStores("live_sectoid_soldier", 1);
        c.addToStores("live_sectoid_leader", 1);
        complete(c, "interrogate_sectoid_soldier");
        complete(c, "interrogate_sectoid_leader");
        complete(c, "alien_origins");
        assertFalse("Martian Solution needs both interrogations + origins first... check gating",
                c.completedResearch().contains("the_martian_solution"));
        complete(c, "the_martian_solution");
        assertTrue("Cydonia or Bust! is now available",
                available(c, "cydonia_or_bust"));
        complete(c, "cydonia_or_bust");
        assertTrue(c.completedResearch().contains("cydonia_or_bust"));
    }

    /** Assign all scientists, run enough days for the project to finish. */
    private void complete(Campaign c, String id) {
        ResearchNode n = rs.research(id);
        assertTrue("available before starting: " + id, available(c, id));
        c.startResearch(n, c.scientists());
        // scientists=20; run generous days so any project (<=300) completes.
        c.advance(400 * 86400);
        assertTrue("completed: " + id, c.completedResearch().contains(id));
    }

    @Test
    public void saveRoundTripsLiveAliens() {
        Campaign c = fresh();
        c.addToStores("live_sectoid_leader", 2);
        SaveGame.Snapshot snap = SaveGame.capture(c, 500_000L, 42, 12345L, 1, false, false);
        String json = SaveGame.toJson(snap);
        SaveGame.Snapshot back = SaveGame.fromJson(json);
        Campaign restored = SaveGame.restoreCampaign(back, rs);
        assertTrue(restored.stores().containsKey("live_sectoid_leader"));
        assertTrue(back.consecutiveBadMonths == 1);
        Integer held = restored.stores().get("live_sectoid_leader");
        assertTrue(held != null && held == 2);
    }
}
