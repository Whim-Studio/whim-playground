package com.whim.xcom.battle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.whim.xcom.model.FireMode;
import com.whim.xcom.rng.Rng;
import com.whim.xcom.rng.SeededRng;
import com.whim.xcom.rules.Ruleset;
import com.whim.xcom.rules.Ruleset1994;
import com.whim.xcom.rules.def.ArmorDef;
import com.whim.xcom.rules.def.WeaponDef;

/**
 * Exercises the pure tactical engine headlessly with a seeded RNG so results are
 * deterministic. Covers deployment, TU-costed movement, line-of-sight, a full
 * fire → damage → death resolution, reaction fire, and an end-to-end battle.
 */
public class BattleEngineTest {

    private final Ruleset rs = Ruleset1994.load();

    private BattleUnit soldier(String name, int tu, int hp, int acc, int rea) {
        WeaponDef rifle = rs.weapon("rifle");
        ArmorDef none = rs.armor("none");
        return new BattleUnit("s_" + name, name, Side.XCOM, tu, hp, acc, rea, 30, rifle, none);
    }

    private BattleUnit alien(String name, int tu, int hp, int acc, int rea) {
        WeaponDef rifle = rs.weapon("rifle");
        return new BattleUnit("a_" + name, name, Side.ALIEN, tu, hp, acc, rea, 30,
                rifle, Armors.uniform("skin", 0));
    }

    @Test
    public void deploymentPlacesBothSquads() {
        BattleGame g = BattleFactory.defaultSkirmish(rs, 7L);
        assertEquals(4, g.living(Side.XCOM).size());
        assertEquals(4, g.living(Side.ALIEN).size());
        assertEquals(16, g.map().width());
    }

    @Test
    public void movementSpendsTimeUnits() {
        BattleMap map = new BattleMap(8, 8);
        BattleGame g = new BattleGame(rs, new SeededRng(1L), map, false);
        BattleUnit s = soldier("Mover", 60, 40, 60, 40);
        s.setPos(0, 0);
        g.addUnit(s);

        int before = s.tu();
        int moved = g.moveUnit(s, 0, 3); // 3 orthogonal steps × 4 TU = 12
        assertTrue("should move at least one tile", moved >= 1);
        assertTrue("TU must be spent", s.tu() < before);
        assertEquals(0, s.x());
        assertTrue(s.y() > 0);
    }

    @Test
    public void wallBlocksLineOfSight() {
        BattleMap map = new BattleMap(8, 3);
        assertTrue(map.hasLineOfSight(0, 1, 7, 1));
        map.tile(4, 1).setKind(Tile.Kind.WALL);
        assertFalse(map.hasLineOfSight(0, 1, 7, 1));
    }

    @Test
    public void aimedFireDamagesAndKills() {
        BattleMap map = new BattleMap(6, 6);
        BattleGame g = new BattleGame(rs, new SeededRng(42L), map, false);
        BattleUnit s = soldier("Sniper", 80, 40, 90, 50);
        s.setPos(2, 2);
        BattleUnit a = alien("Weakling", 40, 10, 40, 30);
        a.setPos(2, 4); // 2 tiles away, clear LOS
        g.addUnit(s);
        g.addUnit(a);

        int shots = 0;
        while (a.alive() && shots < 30) {
            if (!s.hasTU(g.fireCost(s, FireMode.AIMED))) {
                s.refreshTU();
            }
            g.fire(s, a, FireMode.AIMED);
            shots++;
        }
        assertFalse("a weak alien must die under sustained aimed fire", a.alive());
        assertTrue(g.finished());
        assertEquals(Side.XCOM, g.winner());
        assertEquals(1, g.outcome().aliensKilled());
    }

    @Test
    public void reactionFireInterruptsAMover() {
        BattleMap map = new BattleMap(12, 3);
        BattleGame g = new BattleGame(rs, new SeededRng(5L), map, false);
        // Soldier with high reactions and full TU watches the lane.
        BattleUnit watcher = soldier("Overwatch", 60, 40, 70, 90);
        watcher.setPos(6, 0);
        // Alien with low reactions crosses in front of the watcher.
        BattleUnit mover = alien("Runner", 60, 40, 50, 10);
        mover.setPos(0, 1);
        g.addUnit(watcher);
        g.addUnit(mover);

        int watcherTuBefore = watcher.tu();
        g.moveUnit(mover, 11, 1); // walk across the watcher's line of sight
        assertTrue("watcher should have spent TU on reaction fire",
                watcher.tu() < watcherTuBefore);
    }

    @Test
    public void fullBattleTerminatesWithAWinner() {
        BattleGame g = BattleFactory.defaultSkirmish(rs, 3L);
        BattleOutcome outcome = g.autoResolve(60);
        assertNotNull(outcome);
        assertTrue("a 60-turn battle must resolve", outcome.decided());
    }
}
