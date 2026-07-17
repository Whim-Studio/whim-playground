package com.whim.xcom.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.whim.xcom.model.FireMode;
import com.whim.xcom.rules.def.UfoDef;
import com.whim.xcom.rules.def.WeaponDef;

/** Proves the data pack loads from the classpath and populates the registries. */
public class RulesetDataTest {

    private final Ruleset rs = Ruleset1994.load();

    @Test
    public void dataPackLoadedFromClasspath() {
        assertEquals("1994 (X-COM: UFO Defense)", rs.displayName());
        assertTrue("expected several weapons from the data pack", rs.weapons().size() >= 6);
        assertTrue(rs.aliens().size() >= 4);
        assertTrue(rs.facilities().size() >= 8);
        assertTrue(rs.ufos().size() >= 4);
    }

    @Test
    public void weaponFieldsMatchDocumentedTable() {
        WeaponDef rifle = rs.weapon("rifle");
        assertNotNull(rifle);
        assertEquals(110, rifle.accuracyPercent(FireMode.AIMED));
        assertEquals(80, rifle.tuPercent(FireMode.AIMED));
        assertEquals(3, rifle.shots(FireMode.AUTO));
        assertEquals(30, rifle.power());
    }

    @Test
    public void ufoRegistryAddressableById() {
        UfoDef bs = rs.ufo("battleship");
        assertEquals(3200, bs.hullPoints());
        assertEquals(60, bs.mapSize());
    }
}
