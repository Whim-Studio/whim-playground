package com.whim.xcom.rules;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.whim.xcom.model.FireMode;
import com.whim.xcom.rules.def.WeaponDef;
import com.whim.xcom.rules.model.TimeUnitModel;

/** Proves the 1994 TU-cost calculations (walk, diagonal, fire = %maxTU). */
public class TimeUnitModelTest {

    private final Ruleset rs = Ruleset1994.load();
    private final TimeUnitModel tu = rs.timeUnits();

    @Test
    public void walkOrthogonalIsTerrainCost() {
        assertEquals(4, tu.walkCost(4));
    }

    @Test
    public void walkDiagonalIsOnePointFiveTimes() {
        assertEquals(6, tu.walkDiagonalCost(4)); // round(4 × 1.5)
    }

    @Test
    public void fireCostIsPercentOfMaxTu() {
        WeaponDef rifle = rs.weapon("rifle");
        // Aimed = 80% of 60 TU = 48
        assertEquals(48, tu.fireCost(rifle, FireMode.AIMED, 60));
        // Snap = 25% of 60 TU = 15
        assertEquals(15, tu.fireCost(rifle, FireMode.SNAP, 60));
        // Auto = 35% of 60 TU = 21
        assertEquals(21, tu.fireCost(rifle, FireMode.AUTO, 60));
    }

    @Test
    public void turnCostIsPerFortyFiveDegrees() {
        assertEquals(0, tu.turnCost(0));
        assertEquals(3, tu.turnCost(3));
    }

    @Test
    public void unsupportedFireModeCostsNothing() {
        WeaponDef pistol = rs.weapon("pistol");
        assertEquals(0, tu.fireCost(pistol, FireMode.AUTO, 60));
    }
}
