package com.whim.bc3k.sim;

import com.whim.bc3k.sim.combat.GroundSkirmish;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class GroundSkirmishTest {

    @Test public void assaultDamagesTheEnemyForce() {
        GroundSkirmish g = new GroundSkirmish(2);
        int e0 = g.enemyHp();
        g.assault();
        assertTrue(g.enemyHp() < e0);
    }

    @Test public void repeatedAssaultsResolveTheEngagement() {
        GroundSkirmish g = new GroundSkirmish(4);
        int guard = 0;
        while (!g.over() && guard++ < 200) g.assault();
        assertTrue(g.over());
        assertTrue(g.playerWon());          // 4 ATVs out-damage the fixed hostile force
    }

    @Test public void passiveExchangeAlsoResolves() {
        GroundSkirmish g = new GroundSkirmish(3);
        for (int i = 0; i < 2000 && !g.over(); i++) g.tick(0.5);
        assertTrue(g.over());
    }

    @Test public void hpPoolsNeverGoNegative() {
        GroundSkirmish g = new GroundSkirmish(1);
        for (int i = 0; i < 2000; i++) g.tick(1.0);
        assertTrue(g.playerHp() >= 0);
        assertTrue(g.enemyHp() >= 0);
    }
}
