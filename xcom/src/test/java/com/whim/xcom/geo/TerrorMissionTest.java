package com.whim.xcom.geo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import com.whim.xcom.battle.BattleOutcome;
import com.whim.xcom.battle.BattleSetup;
import com.whim.xcom.battle.Side;
import com.whim.xcom.meta.Campaign;
import com.whim.xcom.meta.SoldierRoster;
import com.whim.xcom.model.Difficulty;
import com.whim.xcom.rng.SeededRng;
import com.whim.xcom.rules.Ruleset;
import com.whim.xcom.rules.Ruleset1994;

/**
 * Phase 8 — terror missions: the ignore penalty, assault rewards/penalties, the
 * crew builder and auto-spawn scheduling.
 */
public class TerrorMissionTest {

    private final Ruleset rs = Ruleset1994.load();

    private BattleOutcome victory(java.util.List<String> captures) {
        return new BattleOutcome(Side.XCOM, 6,
                new ArrayList<String>(Arrays.asList("Sgt. Vasquez")),
                new ArrayList<String>(), 4, captures, false);
    }

    private BattleOutcome defeat() {
        return new BattleOutcome(Side.ALIEN, 8, new ArrayList<String>(),
                new ArrayList<String>(Arrays.asList("Pvt. Novak")), 1);
    }

    /** An ignored terror site expires and inflicts exactly the ignore penalty. */
    @Test
    public void ignoredTerrorSiteAppliesPenalty() {
        Base bare = new Base("HQ", 0.5, 0.5);
        bare.addFacility(rs.facility("access_lift")); // no radar → no UFO detection noise
        GeoGame g = new GeoGame(rs, new SeededRng(1L), bare);
        g.deployTerrorSite(0.5, 0.5, 1L); // already effectively due
        g.clock().setSpeed(GeoClock.Speed.MIN5);
        g.tick(); // 300s step: the only scoring event is the terror expiry
        assertEquals("ignore penalty applied", GeoGame.TERROR_IGNORE_PENALTY, g.totalScore());
        assertEquals("site cleared after expiry", 0, g.activeTerrorCount());
        assertTrue(g.terrorSites().isEmpty());
    }

    /** Assaulting and winning a terror site awards score and holds live captives. */
    @Test
    public void wonTerrorAssaultRewardsAndCaptures() {
        GeoGame g = GeoFactory.defaultCampaign(rs, 2L); // default base has containment
        TerrorSite site = g.deployTerrorSite(0.4, 0.4, 48 * 3600L);
        int before = g.totalScore();
        g.resolveTerror(site, victory(Arrays.asList("sectoid_leader")));
        assertTrue("score rose for a defended city", g.totalScore() > before);
        assertEquals("live captive secured", 1, g.liveAlienCount());
        assertEquals("site consumed", 0, g.activeTerrorCount());
    }

    /** A lost terror assault costs score and still clears the site. */
    @Test
    public void lostTerrorAssaultCostsScore() {
        GeoGame g = GeoFactory.defaultCampaign(rs, 3L);
        TerrorSite site = g.deployTerrorSite(0.4, 0.4, 48 * 3600L);
        int before = g.totalScore();
        g.resolveTerror(site, defeat());
        assertTrue("score fell for a lost city", g.totalScore() < before);
        assertEquals(0, g.activeTerrorCount());
    }

    /** The terror crew builder yields a full X-COM squad and a tough alien force. */
    @Test
    public void terrorAssaultBuildsBothSides() {
        GeoGame g = GeoFactory.defaultCampaign(rs, 4L);
        g.setDifficulty(Difficulty.VETERAN);
        TerrorSite site = g.deployTerrorSite(0.4, 0.4, 48 * 3600L);
        BattleSetup setup = g.buildTerrorAssault(site, 99L);
        assertFalse("squad deployed", setup.soldiers().isEmpty());
        assertTrue("large terror crew", setup.aliens().size() >= 6);
        assertTrue("terror missions are fought at night", setup.night());
    }

    /** Left to run, the Geoscape spawns a terror mission within the first game-month. */
    @Test
    public void terrorMissionsSpawnOverTime() {
        GeoGame g = GeoFactory.defaultCampaign(rs, 5L);
        g.clock().setSpeed(GeoClock.Speed.DAY1);
        boolean seen = false;
        for (int day = 0; day < 20 && !g.gameOver(); day++) {
            g.tick();
            if (g.activeTerrorCount() > 0) {
                seen = true;
                break;
            }
        }
        assertTrue("a terror mission appeared within 20 game-days", seen);
    }
}
