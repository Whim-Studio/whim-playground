package com.whim.bc3k.sim;

import com.whim.bc3k.api.Enums;
import com.whim.bc3k.sim.combat.CombatSim;
import com.whim.bc3k.sim.ship.ShipSystems;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CombatSimTest {

    @Test public void playerVolleysDestroyEnemyAndWin() {
        ShipSystems player = new ShipSystems();
        CombatSim c = new CombatSim(player, "Gammulan Raider");
        for (int i = 0; i < 30 && !c.over(); i++) c.playerVolley();
        assertTrue(c.over());
        assertTrue(c.playerWon());
        assertTrue(c.enemy().destroyed());
    }

    @Test public void unpoweredWeaponsDealNoDamage() {
        ShipSystems player = new ShipSystems();
        // Cut all weapon power: volleys should do nothing.
        player.allocate(Enums.PowerSystem.WEAPONS, -player.system(Enums.PowerSystem.WEAPONS).power());
        CombatSim c = new CombatSim(player, "Raider");
        double dmg = c.playerVolley();
        assertTrue(dmg == 0.0);
        assertFalse(c.over());
    }

    @Test public void enemyAutoFireDamagesPlayerOverTime() {
        ShipSystems player = new ShipSystems();
        int shields0 = player.shields();
        CombatSim c = new CombatSim(player, "Raider");
        // Advance several enemy cadences without the player firing.
        for (int i = 0; i < 40; i++) c.tick(0.25);   // 10s > several 1.5s cadences
        assertTrue(player.shields() < shields0 || player.hull() < player.maxHull());
    }

    @Test public void committedFightersDamageTheEnemyOverTime() {
        ShipSystems player = new ShipSystems();
        CombatSim c = new CombatSim(player, "Raider");
        c.addPlayerFighters(4);
        int enemyShield0 = c.enemy().shields();
        for (int i = 0; i < 20; i++) c.tick(0.25);   // 5s of dogfight, no capital volleys fired
        assertTrue(c.enemy().shields() < enemyShield0 || c.enemy().hull() < c.enemy().maxHull());
    }

    @Test public void fighterWingsAttriteEachOther() {
        ShipSystems player = new ShipSystems();
        CombatSim c = new CombatSim(player, "Raider");
        c.addPlayerFighters(3);
        int pf0 = c.playerFighters(), ef0 = c.enemyFighters();
        for (int i = 0; i < 60; i++) c.tick(0.25);   // 15s
        assertTrue(c.playerFighters() <= pf0);
        assertTrue(c.enemyFighters() <= ef0);
    }

    @Test public void higherWeaponPowerKillsFaster() {
        ShipSystems weak = new ShipSystems();
        ShipSystems strong = new ShipSystems();
        strong.allocate(Enums.PowerSystem.WEAPONS, 15);   // more weapon power

        CombatSim cw = new CombatSim(weak, "R");
        CombatSim cs = new CombatSim(strong, "R");
        int weakShots = 0, strongShots = 0;
        while (!cw.over() && weakShots < 100) { cw.playerVolley(); weakShots++; }
        while (!cs.over() && strongShots < 100) { cs.playerVolley(); strongShots++; }
        assertTrue(strongShots < weakShots);
    }
}
