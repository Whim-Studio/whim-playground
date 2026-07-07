package com.whim.b5wars.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.whim.b5wars.model.Facing;
import com.whim.b5wars.model.Faction;
import com.whim.b5wars.model.Race;
import com.whim.b5wars.model.Scenario;
import com.whim.b5wars.model.ShipClass;
import com.whim.b5wars.model.VictoryCondition;
import com.whim.b5wars.model.Weapon;
import com.whim.b5wars.model.WeaponTrait;

import java.util.List;
import java.util.Map;

import org.junit.Test;

/** Confirms every bundled JSON resource parses into model types without error. */
public class DataLoaderTest {

    @Test
    public void loadsAllFactions() {
        List<Faction> factions = DataLoader.loadFactions();
        assertEquals(2, factions.size());
        for (int i = 0; i < factions.size(); i++) {
            assertFalse(factions.get(i).getShipClasses().isEmpty());
        }
    }

    @Test
    public void loadsHyperion() {
        Faction ea = DataLoader.loadFaction("/factions/earth-alliance.json");
        assertEquals(Race.EARTH_ALLIANCE, ea.getRace());
        ShipClass hyperion = ea.byId("hyperion");
        assertNotNull(hyperion);
        assertTrue(hyperion.getWeapons().size() >= 3);
        Weapon beam = hyperion.getWeapons().get(0);
        assertTrue(beam.has(WeaponTrait.ARMOR_PIERCING));
        assertTrue(hyperion.getArmor().containsKey(Facing.F));
    }

    @Test
    public void loadsGQuan() {
        Faction narn = DataLoader.loadFaction("/factions/narn-regime.json");
        assertEquals(Race.NARN_REGIME, narn.getRace());
        ShipClass gquan = narn.byId("gquan");
        assertNotNull(gquan);
        assertEquals(Race.NARN_REGIME, gquan.getRace());
    }

    @Test
    public void loadsScenario() {
        Scenario s = DataLoader.loadScenario("/scenarios/border-skirmish.json");
        assertEquals(VictoryCondition.DESTROY_OR_CRIPPLE_ENEMY, s.getVictory());
        assertEquals(12, s.getTurnLimit());
        assertEquals(2, s.getPlacements().size());
    }

    @Test
    public void loadsImpulseCadence() {
        Map<Integer, boolean[]> cadence = DataLoader.loadImpulseCadence();
        assertTrue(cadence.containsKey(Integer.valueOf(6)));
        boolean[] speed6 = cadence.get(Integer.valueOf(6));
        assertEquals(8, speed6.length);
        int moves = 0;
        for (int i = 0; i < speed6.length; i++) {
            if (speed6[i]) {
                moves++;
            }
        }
        assertEquals(6, moves);
    }

    @Test
    public void loadsCriticalTable() {
        List<CriticalEntry> table = DataLoader.loadCriticalTable();
        assertFalse(table.isEmpty());
        assertEquals(20, table.get(table.size() - 1).getRollMax());
    }
}
