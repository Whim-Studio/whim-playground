package com.whim.xcom.battle;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.whim.xcom.rng.SeededRng;
import com.whim.xcom.rules.Ruleset;
import com.whim.xcom.rules.Ruleset1994;
import com.whim.xcom.rules.model.PsiModel;

/**
 * Phase 5: the pluggable psi model, a psi attack that panics a soldier, and the
 * panic effect (a cowering soldier loses its turn).
 */
public class PsiTest {

    private final Ruleset rs = Ruleset1994.load();

    @Test
    public void psiModelScalesWithStrengthAndDistance() {
        PsiModel psi = rs.psi();
        assertTrue(psi.panicChancePercent(60, 20, 1) > psi.panicChancePercent(60, 20, 8));
        assertTrue(psi.panicChancePercent(60, 0, 1) > psi.panicChancePercent(60, 40, 1));
        assertTrue("no psi ability -> zero", psi.panicChancePercent(0, 10, 1) == 0);
    }

    @Test
    public void psiAttackPanicsAWeakMindedSoldier() {
        BattleMap map = new BattleMap(6, 3);
        BattleGame g = new BattleGame(rs, new SeededRng(1L), map, false);
        BattleUnit soldier = new BattleUnit("s", "Rookie", Side.XCOM, 50, 30, 55, 45, 30,
                rs.weapon("rifle"), rs.armor("none"));
        soldier.setPsiStrength(0); // very weak-minded → high panic chance
        soldier.setPos(1, 1);
        BattleUnit leader = new BattleUnit("a", "Sectoid Leader", Side.ALIEN, 60, 32, 65, 72, 32,
                rs.weapon("rifle"), Armors.uniform("skin", 12));
        leader.setPsiStrength(80);
        leader.setPos(3, 1);
        g.addUnit(soldier);
        g.addUnit(leader);

        assertTrue(g.psiAttack(leader, soldier));
        assertTrue("a strong psi attack on a weak mind should panic", soldier.panicked());
    }

    @Test
    public void panickedSoldierLosesItsTurn() {
        BattleMap map = new BattleMap(6, 3);
        // A full wall column separates the two so no combat perturbs the turn.
        for (int y = 0; y < 3; y++) {
            map.tile(3, y).setKind(Tile.Kind.WALL);
        }
        BattleGame g = new BattleGame(rs, new SeededRng(2L), map, false);
        BattleUnit soldier = new BattleUnit("s", "Rookie", Side.XCOM, 50, 30, 55, 45, 30,
                rs.weapon("rifle"), rs.armor("none"));
        soldier.setPos(1, 1);
        BattleUnit alien = new BattleUnit("a", "Sectoid", Side.ALIEN, 50, 100, 40, 10, 30,
                rs.weapon("rifle"), Armors.uniform("skin", 0));
        alien.setPos(4, 1);
        g.addUnit(soldier);
        g.addUnit(alien);

        soldier.setPanicked(true);
        g.endTurn(); // aliens act, then panic is processed as the player turn begins
        assertFalse("panic flag cleared after cowering", soldier.panicked());
        assertTrue("a cowering soldier has spent all its TU", soldier.tu() == 0);
    }
}
