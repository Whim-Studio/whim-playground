package com.whim.xcom.geo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.whim.xcom.battle.BattleSetup;
import com.whim.xcom.meta.Campaign;
import com.whim.xcom.meta.Soldier;
import com.whim.xcom.meta.SoldierRoster;
import com.whim.xcom.rng.SeededRng;
import com.whim.xcom.rules.Ruleset;
import com.whim.xcom.rules.Ruleset1994;

/**
 * Phase 8 — hardening: missions must never launch with an empty X-COM side, even
 * when every soldier on the roster is wounded (which would otherwise deploy zero
 * units and crash or auto-lose the battle).
 */
public class HardeningTest {

    private final Ruleset rs = Ruleset1994.load();

    @Test
    public void allWoundedRosterStillDeploysAScratchSquad() {
        SoldierRoster roster = new SoldierRoster();
        Soldier a = new Soldier("Pvt. Hurt", 50, 30, 50, 40, 30);
        Soldier b = new Soldier("Pvt. Ache", 50, 30, 50, 40, 30);
        a.wound(10);
        b.wound(10);
        roster.add(a);
        roster.add(b);

        GeoGame g = new GeoGame(rs, new SeededRng(1L), baseWithContainment());
        g.setCampaign(new Campaign(5, 5, roster));
        Ufo u = g.deployUfo(rs.ufo("small_scout"), 0.5, 0.5);

        BattleSetup setup = g.buildAssault(u, 7L);
        assertFalse("no deployable soldiers -> scratch squad, never empty", setup.soldiers().isEmpty());
        assertTrue("aliens present", setup.aliens().size() > 0);
    }

    @Test
    public void terrorAssaultAlsoNeverLaunchesEmpty() {
        GeoGame g = new GeoGame(rs, new SeededRng(2L), baseWithContainment());
        // No campaign at all: a default scratch squad must still deploy.
        TerrorSite site = g.deployTerrorSite(0.5, 0.5, 48 * 3600L);
        BattleSetup setup = g.buildTerrorAssault(site, 8L);
        assertFalse(setup.soldiers().isEmpty());
    }

    private Base baseWithContainment() {
        Base base = new Base("HQ", 0.5, 0.5);
        base.addFacility(rs.facility("access_lift"));
        base.addFacility(rs.facility("large_radar"));
        base.addFacility(rs.facility("alien_containment"));
        return base;
    }
}
