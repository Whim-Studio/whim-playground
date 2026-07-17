package com.whim.xcom.battle;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.whim.xcom.model.Difficulty;
import com.whim.xcom.rng.SeededRng;
import com.whim.xcom.rules.Ruleset;
import com.whim.xcom.rules.Ruleset1994;

/**
 * Phase 4: grenades destroy terrain and damage units, throwing respects TU/range/
 * ammo, and difficulty scales alien stats.
 */
public class ExplosivesTest {

    private final Ruleset rs = Ruleset1994.load();

    private BattleUnit soldier(int x, int y, int grenades) {
        BattleUnit u = new BattleUnit("s", "Thrower", Side.XCOM, 60, 40, 60, 45, 40,
                rs.weapon("rifle"), rs.armor("none"));
        u.setPos(x, y);
        u.setGrenades(grenades);
        return u;
    }

    @Test
    public void grenadeDestroysWallAndClearsLineOfSight() {
        BattleMap map = new BattleMap(9, 3);
        map.tile(4, 1).setKind(Tile.Kind.WALL);
        BattleGame g = new BattleGame(rs, new SeededRng(1L), map, false);
        BattleUnit s = soldier(1, 1, 1);
        g.addUnit(s);

        assertFalse(map.hasLineOfSight(0, 1, 8, 1)); // wall blocks
        assertTrue(g.throwGrenade(s, 4, 1));         // blow it up
        assertTrue("wall should be cleared to rubble", map.hasLineOfSight(0, 1, 8, 1));
        assertTrue("grenade consumed", s.grenades() == 0);
        assertTrue("TU spent", s.tu() < s.maxTU());
    }

    @Test
    public void grenadeDamagesUnitsInBlast() {
        BattleMap map = new BattleMap(9, 3);
        BattleGame g = new BattleGame(rs, new SeededRng(3L), map, false);
        BattleUnit s = soldier(1, 1, 1);
        BattleUnit alien = new BattleUnit("a", "Sectoid", Side.ALIEN, 50, 30, 50, 50, 30,
                rs.weapon("rifle"), Armors.uniform("skin", 0));
        alien.setPos(4, 1);
        g.addUnit(s);
        g.addUnit(alien);

        int before = alien.health();
        g.throwGrenade(s, 4, 1);
        assertTrue("alien should take blast damage", alien.health() < before);
    }

    @Test
    public void cannotThrowWithoutGrenades() {
        BattleMap map = new BattleMap(9, 3);
        BattleGame g = new BattleGame(rs, new SeededRng(1L), map, false);
        BattleUnit s = soldier(1, 1, 0);
        g.addUnit(s);
        assertFalse(g.throwGrenade(s, 4, 1));
    }

    @Test
    public void difficultyScalesAlienStats() {
        BattleSetup easy = new BattleSetup().mapSize(10, 10).seed(1L).difficulty(Difficulty.BEGINNER);
        easy.addSoldier(BattleSetup.UnitSpec.soldier("S", "rifle", "none", 60, 55, 34, 45, 30));
        easy.addAlien(BattleSetup.UnitSpec.alien("sectoid_soldier", "rifle"));

        BattleSetup hard = new BattleSetup().mapSize(10, 10).seed(1L).difficulty(Difficulty.SUPERHUMAN);
        hard.addSoldier(BattleSetup.UnitSpec.soldier("S", "rifle", "none", 60, 55, 34, 45, 30));
        hard.addAlien(BattleSetup.UnitSpec.alien("sectoid_soldier", "rifle"));

        BattleUnit easyAlien = firstAlien(BattleFactory.build(rs, easy));
        BattleUnit hardAlien = firstAlien(BattleFactory.build(rs, hard));
        assertTrue("Superhuman aliens should be tougher than Beginner",
                hardAlien.maxHealth() > easyAlien.maxHealth());
    }

    private BattleUnit firstAlien(BattleGame g) {
        return g.living(Side.ALIEN).get(0);
    }
}
