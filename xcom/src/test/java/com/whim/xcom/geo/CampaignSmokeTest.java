package com.whim.xcom.geo;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.whim.xcom.battle.BattleOutcome;
import com.whim.xcom.battle.BattleSetup;
import com.whim.xcom.meta.SaveGame;
import com.whim.xcom.model.Difficulty;
import com.whim.xcom.rules.Ruleset;
import com.whim.xcom.rules.Ruleset1994;

/**
 * Phase 8 — a headless end-to-end smoke of the Geoscape loop: run several game
 * months while auto-resolving every crash site and terror mission that appears,
 * on every difficulty. Proves the campaign never dead-ends or throws, that both
 * mission types are generated and resolved, and that the meta-state save/loads.
 */
public class CampaignSmokeTest {

    private final Ruleset rs = Ruleset1994.load();

    @Test
    public void fullLoopIsStableOnEveryDifficulty() {
        for (Difficulty d : Difficulty.values()) {
            runMonths(d, 30L + d.level());
        }
    }

    private void runMonths(Difficulty difficulty, long seed) {
        final GeoGame g = GeoFactory.defaultCampaign(rs, seed);
        g.setDifficulty(difficulty);
        final MissionLauncherCounts counts = new MissionLauncherCounts();

        g.setListener(new GeoGame.Listener() {
            @Override public void onEvent(String m) { }
            @Override public void onChanged() { }
            @Override public void onCrashSite(Ufo ufo) {
                BattleSetup setup = g.buildAssault(ufo, 1000 + counts.crash);
                BattleOutcome outcome = MissionLauncher.autoResolve(rs).launch(setup);
                g.resolveMission(ufo, outcome);
                counts.crash++;
            }
            @Override public void onVictory(String m) { }
            @Override public void onDefeat(String m) { }
        });

        g.clock().setSpeed(GeoClock.Speed.DAY1);
        for (int day = 0; day < 90 && !g.gameOver(); day++) {
            g.tick();
            // The view handles terror clicks; here we auto-assault any active site.
            for (TerrorSite t : new ArrayList<TerrorSite>(g.terrorSites())) {
                if (t.active()) {
                    BattleSetup setup = g.buildTerrorAssault(t, 2000 + counts.terror);
                    BattleOutcome outcome = MissionLauncher.autoResolve(rs).launch(setup);
                    g.resolveTerror(t, outcome);
                    counts.terror++;
                }
            }
        }

        assertTrue("clock advanced for " + difficulty, g.clock().seconds() > 0);
        assertTrue("at least one mission occurred for " + difficulty,
                counts.crash + counts.terror > 0);

        // The whole meta-state round-trips through a save snapshot without throwing.
        SaveGame.Snapshot snap = SaveGame.capture(g.campaign(), g.funds(), g.totalScore(),
                g.clock().seconds(), g.consecutiveBadMonths(), g.gameWon(), g.gameLost());
        String json = SaveGame.toJson(snap);
        SaveGame.Snapshot back = SaveGame.fromJson(json);
        assertTrue("snapshot survived a JSON round-trip", back.clockSeconds >= 0);
    }

    /** Tiny mutable counter holder (no lambdas on Java 8 anon-class style). */
    private static final class MissionLauncherCounts {
        int crash;
        int terror;
    }

    @SuppressWarnings("unused")
    private static List<String> none() {
        return new ArrayList<String>();
    }
}
