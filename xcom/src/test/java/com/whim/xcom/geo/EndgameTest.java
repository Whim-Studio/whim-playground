package com.whim.xcom.geo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import com.whim.xcom.battle.BattleOutcome;
import com.whim.xcom.battle.Side;
import com.whim.xcom.meta.Campaign;
import com.whim.xcom.meta.SoldierRoster;
import com.whim.xcom.rng.SeededRng;
import com.whim.xcom.rules.Ruleset;
import com.whim.xcom.rules.Ruleset1994;

/**
 * Phase 7 — the Geoscape endgame: live-alien recovery gated on Alien Containment,
 * the Cydonia victory trigger, and the Council-termination loss condition.
 */
public class EndgameTest {

    private final Ruleset rs = Ruleset1994.load();

    private BattleOutcome victory(java.util.List<String> captures, boolean brainKilled) {
        return new BattleOutcome(Side.XCOM, 6,
                new ArrayList<String>(Arrays.asList("Sgt. Vasquez")),
                new ArrayList<String>(), 3, captures, brainKilled);
    }

    @Test
    public void containmentRequiredToHoldLiveAliens() {
        // Default base includes Alien Containment.
        GeoGame withHold = GeoFactory.defaultCampaign(rs, 5L);
        Ufo u = withHold.deployUfo(rs.ufo("small_scout"), 0.5, 0.5);
        withHold.resolveMission(u, victory(Arrays.asList("sectoid_soldier"), false));
        assertEquals("captive held in containment", 1, withHold.liveAlienCount());
        assertTrue(withHold.campaign().stores().containsKey("live_sectoid_soldier"));

        // A base with no containment cannot hold the captive.
        Base bare = new Base("Outpost", 0.5, 0.5);
        bare.addFacility(rs.facility("access_lift"));
        bare.addFacility(rs.facility("small_radar"));
        GeoGame noHold = new GeoGame(rs, new SeededRng(5L), bare);
        noHold.setCampaign(new Campaign(5, 5, new SoldierRoster()));
        Ufo u2 = noHold.deployUfo(rs.ufo("small_scout"), 0.5, 0.5);
        noHold.resolveMission(u2, victory(Arrays.asList("sectoid_soldier"), false));
        assertEquals("no containment → captive lost", 0, noHold.liveAlienCount());
    }

    @Test
    public void cydoniaFinalStageWinsTheGame() {
        GeoGame g = GeoFactory.defaultCampaign(rs, 6L);
        final boolean[] won = {false};
        g.setListener(new GeoGame.Listener() {
            @Override public void onEvent(String m) { }
            @Override public void onChanged() { }
            @Override public void onCrashSite(Ufo ufo) { }
            @Override public void onVictory(String m) { won[0] = true; }
            @Override public void onDefeat(String m) { }
        });
        // Surface stage cleared but not final — game continues.
        assertFalse(g.resolveCydonia(victory(new ArrayList<String>(), false), false));
        assertFalse(g.gameWon());
        // Alien-base stage cleared (Brain killed) — victory.
        assertTrue(g.resolveCydonia(victory(new ArrayList<String>(), true), true));
        assertTrue(g.gameWon());
        assertTrue(g.gameOver());
        assertTrue("victory screen fired", won[0]);
    }

    @Test
    public void sustainedPoorPerformanceTerminatesXcom() {
        // A base whose upkeep it cannot pay: funds start at zero, no funding nations.
        Base base = new Base("HQ", 0.5, 0.5);
        base.addFacility(rs.facility("access_lift"));   // upkeep, no radar → no score churn
        GeoGame g = new GeoGame(rs, new SeededRng(7L), base);
        g.setCampaign(new Campaign(1, 1, new SoldierRoster()));
        g.restoreState(0L, 0, 0L); // broke from month one
        final boolean[] defeat = {false};
        g.setListener(new GeoGame.Listener() {
            @Override public void onEvent(String m) { }
            @Override public void onChanged() { }
            @Override public void onCrashSite(Ufo ufo) { }
            @Override public void onVictory(String m) { }
            @Override public void onDefeat(String m) { defeat[0] = true; }
        });
        g.clock().setSpeed(GeoClock.Speed.DAY1);
        for (int i = 0; i < 80 && !g.gameLost(); i++) {
            g.tick();
        }
        assertTrue("two poor months → Council terminates X-COM", g.gameLost());
        assertTrue("defeat screen fired", defeat[0]);
        assertTrue(g.gameOver());
    }
}
