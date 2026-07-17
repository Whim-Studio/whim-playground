package com.whim.xcom.geo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.whim.xcom.battle.BattleOutcome;
import com.whim.xcom.rng.SeededRng;
import com.whim.xcom.rules.Ruleset;
import com.whim.xcom.rules.Ruleset1994;

/**
 * Headless, deterministic tests for the Geoscape engine: the clock, UFO spawn +
 * radar detection, interceptor air combat producing a crash site, the monthly
 * funding review, and the ground-assault handoff.
 */
public class GeoEngineTest {

    private final Ruleset rs = Ruleset1994.load();

    @Test
    public void clockAdvancesByCurrentSpeed() {
        GeoClock clock = new GeoClock();
        clock.setSpeed(GeoClock.Speed.MIN30);
        clock.tick();
        assertEquals(1800, clock.seconds());
        clock.setSpeed(GeoClock.Speed.PAUSE);
        clock.tick();
        assertEquals(1800, clock.seconds()); // paused: no advance
    }

    @Test
    public void radarDetectsASpawnedUfo() {
        GeoGame g = GeoFactory.defaultCampaign(rs, 11L);
        g.clock().setSpeed(GeoClock.Speed.DAY1);
        boolean anyDetected = false;
        for (int i = 0; i < 40 && !anyDetected; i++) {
            g.tick();
            for (Ufo u : g.ufos()) {
                if (u.detected()) {
                    anyDetected = true;
                    break;
                }
            }
        }
        assertTrue("radar should eventually detect a UFO", anyDetected);
    }

    @Test
    public void interceptorShootsDownScoutAndRaisesCrashSite() {
        GeoGame g = GeoFactory.defaultCampaign(rs, 2L);
        final AtomicReference<Ufo> crash = new AtomicReference<Ufo>();
        g.setListener(new GeoGame.Listener() {
            @Override public void onEvent(String message) { }
            @Override public void onChanged() { }
            @Override public void onCrashSite(Ufo ufo) { crash.set(ufo); }
            @Override public void onVictory(String message) { }
            @Override public void onDefeat(String message) { }
        });
        Ufo scout = g.deployUfo(rs.ufo("small_scout"), g.base().x(), g.base().y());
        scout.setDetected(true);
        assertTrue(g.intercept(scout));
        g.clock().setSpeed(GeoClock.Speed.HOUR1);
        for (int i = 0; i < 20 && crash.get() == null; i++) {
            g.tick();
        }
        assertTrue("a downed scout must raise a crash site", crash.get() != null);
        assertEquals(Ufo.Status.CRASHED, scout.status());
    }

    @Test
    public void monthlyFundingReviewRespondsToScore() {
        FundingNation good = new FundingNation("Testland", 400_000);
        good.addScore(300);
        int after = good.applyMonthlyReview();
        assertTrue("good score should raise funding", after > 400_000);

        FundingNation bad = new FundingNation("Failistan", 400_000);
        bad.addScore(-100);
        assertTrue("poor score should cut funding", bad.applyMonthlyReview() < 400_000);

        FundingNation quitter = new FundingNation("Quitopia", 400_000);
        quitter.addScore(-500);
        quitter.applyMonthlyReview();
        assertTrue(quitter.withdrawn());
    }

    @Test
    public void monthlyRolloverProducesAReport() {
        GeoGame g = GeoFactory.defaultCampaign(rs, 4L);
        final AtomicBoolean reported = new AtomicBoolean(false);
        g.setListener(new GeoGame.Listener() {
            @Override public void onEvent(String message) {
                if (message.contains("Monthly Report")) {
                    reported.set(true);
                }
            }
            @Override public void onChanged() { }
            @Override public void onCrashSite(Ufo ufo) { }
            @Override public void onVictory(String message) { }
            @Override public void onDefeat(String message) { }
        });
        g.clock().setSpeed(GeoClock.Speed.DAY1);
        for (int i = 0; i < 31; i++) {
            g.tick();
        }
        assertTrue("a month of game time should trigger a Council report", reported.get());
    }

    @Test
    public void assaultHandoffProducesADecidedBattle() {
        GeoGame g = GeoFactory.defaultCampaign(rs, 9L);
        Ufo scout = g.deployUfo(rs.ufo("small_scout"), 0.5, 0.5);
        MissionLauncher launcher = MissionLauncher.autoResolve(rs);
        BattleOutcome outcome = launcher.launch(g.buildAssault(scout, 1L));
        assertTrue("the assault must resolve to a winner", outcome.decided());
        assertFalse(g.buildAssault(scout, 1L).soldiers().isEmpty());
    }
}
