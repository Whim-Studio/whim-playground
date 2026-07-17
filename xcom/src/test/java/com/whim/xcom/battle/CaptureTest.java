package com.whim.xcom.battle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.whim.xcom.model.FireMode;
import com.whim.xcom.rng.SeededRng;
import com.whim.xcom.rules.Ruleset;
import com.whim.xcom.rules.Ruleset1994;

/**
 * Phase 7 — live capture on the Battlescape. A stun weapon accrues stun damage;
 * an alien knocked unconscious (not killed) on a field X-COM controls is captured
 * alive and surfaces in the {@link BattleOutcome}. Lethal damage produces no capture.
 */
public class CaptureTest {

    private final Ruleset rs = Ruleset1994.load();

    @Test
    public void stunAccruesAndKnocksOutWithoutKilling() {
        BattleUnit u = new BattleUnit("A0", "Sectoid", Side.ALIEN,
                54, 30, 52, 63, 30, rs.weapon("stun_rod"), rs.armor("none"));
        assertTrue(u.conscious());
        u.applyStun(20);
        assertTrue("20 < 30 health — still up", u.conscious());
        u.applyStun(20); // 40 >= 30
        assertTrue(u.unconscious());
        assertFalse(u.conscious());
        assertTrue("unconscious is still alive (capturable)", u.alive());
    }

    @Test
    public void killedUnitIsNotUnconscious() {
        BattleUnit u = new BattleUnit("A0", "Sectoid", Side.ALIEN,
                54, 30, 52, 63, 30, rs.weapon("rifle"), rs.armor("none"));
        u.applyDamage(30);
        assertFalse(u.alive());
        assertFalse(u.unconscious());
    }

    @Test
    public void stunningTheLastAlienCapturesItLiveOnVictory() {
        BattleMap map = new BattleMap(6, 6);
        BattleGame game = new BattleGame(rs, new SeededRng(1L), map, false);

        BattleUnit soldier = new BattleUnit("S0", "Cpl. Tanaka", Side.XCOM,
                60, 40, 90, 50, 40, rs.weapon("stun_rod"), rs.armor("none"));
        soldier.setPos(2, 3);
        game.addUnit(soldier);

        BattleUnit alien = new BattleUnit("A0", "Sectoid", Side.ALIEN,
                54, 30, 52, 63, 30, rs.weapon("rifle"), rs.armor("none"));
        alien.setAlienDefId("sectoid_soldier");
        alien.setPos(2, 2); // adjacent, in line of sight
        game.addUnit(alien);

        // Batter it with the stun rod until it collapses (100%-accuracy melee).
        for (int i = 0; i < 40 && alien.conscious(); i++) {
            soldier.refreshTU();
            game.fire(soldier, alien, FireMode.SNAP);
        }
        assertTrue("stun rod should knock the sectoid out", alien.unconscious());
        assertTrue("no conscious aliens left → X-COM wins", game.finished());
        assertEquals(Side.XCOM, game.winner());

        BattleOutcome outcome = game.outcome();
        assertTrue(outcome.xcomVictory());
        assertTrue("the stunned sectoid is captured alive",
                outcome.liveCaptures().contains("sectoid_soldier"));
        assertEquals("a stunned alien is not counted as killed", 0, outcome.aliensKilled());
    }
}
