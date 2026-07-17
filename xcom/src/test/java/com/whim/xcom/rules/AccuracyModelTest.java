package com.whim.xcom.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.whim.xcom.model.FireMode;
import com.whim.xcom.rules.def.WeaponDef;
import com.whim.xcom.rules.model.AccuracyModel;
import com.whim.xcom.rules.model.ShotContext;

/**
 * Proves the 1994 accuracy formula: {@code firingAccuracy × weaponAccuracy / 100},
 * plus the kneel and one-handed multipliers.
 */
public class AccuracyModelTest {

    private final Ruleset rs = Ruleset1994.load();
    private final AccuracyModel acc = rs.accuracy();

    @Test
    public void rifleAimedStanding() {
        WeaponDef rifle = rs.weapon("rifle");
        // 60 × 110 / 100 = 66
        int hit = acc.hitChancePercent(rifle, FireMode.AIMED, ShotContext.basic(60));
        assertEquals(66, hit);
    }

    @Test
    public void rifleSnapStanding() {
        WeaponDef rifle = rs.weapon("rifle");
        // 55 × 60 / 100 = 33
        int hit = acc.hitChancePercent(rifle, FireMode.SNAP, ShotContext.basic(55));
        assertEquals(33, hit);
    }

    @Test
    public void kneelingAddsFifteenPercent() {
        WeaponDef rifle = rs.weapon("rifle");
        ShotContext kneel = new ShotContext(60, true, true, true, 0, 1, false);
        // 66 × 1.15 = 75.9 -> floor 75
        assertEquals(75, acc.hitChancePercent(rifle, FireMode.AIMED, kneel));
    }

    @Test
    public void oneHandedTwoHandedWeaponPenalised() {
        WeaponDef rifle = rs.weapon("rifle"); // two-handed
        ShotContext oneHand = new ShotContext(60, false, true, false, 0, 1, false);
        // 66 × 0.8 = 52.8 -> floor 52
        assertEquals(52, acc.hitChancePercent(rifle, FireMode.AIMED, oneHand));
    }

    @Test
    public void unsupportedModeIsZero() {
        WeaponDef pistol = rs.weapon("pistol"); // no auto
        assertEquals(0, acc.hitChancePercent(pistol, FireMode.AUTO, ShotContext.basic(60)));
    }

    @Test
    public void pistolHasNoAutoButHasSnapAndAimed() {
        WeaponDef pistol = rs.weapon("pistol");
        assertTrue(pistol.supports(FireMode.SNAP));
        assertTrue(pistol.supports(FireMode.AIMED));
        assertTrue(!pistol.supports(FireMode.AUTO));
    }
}
