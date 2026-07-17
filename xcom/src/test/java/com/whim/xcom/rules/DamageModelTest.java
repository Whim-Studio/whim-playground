package com.whim.xcom.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.whim.xcom.model.DamageType;
import com.whim.xcom.rng.Rng;
import com.whim.xcom.rng.SeededRng;
import com.whim.xcom.rules.def.ArmorDef;
import com.whim.xcom.rules.model.DamageModel;
import com.whim.xcom.rules.model.DamageModel.Facing;

/**
 * Proves the 1994 damage roll: {@code power × roll(0..200)/100 − armour},
 * both the deterministic {@code applyDamage} form and the seeded-RNG form.
 */
public class DamageModelTest {

    private final Ruleset rs = Ruleset1994.load();
    private final DamageModel dmg = rs.damage();

    @Test
    public void meanRollEqualsPowerMinusArmor() {
        ArmorDef none = rs.armor("none");
        // 100% roll of power 30 through 0 armour = 30
        assertEquals(30, dmg.applyDamage(100, 30, DamageType.ARMOR_PIERCING, none, Facing.FRONT));
    }

    @Test
    public void armorIsSubtractedOnStruckFacing() {
        ArmorDef armor = rs.armor("personal_armor"); // front 50
        // 200% roll of power 30 = 60; minus 50 front = 10
        assertEquals(10, dmg.applyDamage(200, 30, DamageType.ARMOR_PIERCING, armor, Facing.FRONT));
    }

    @Test
    public void damageNeverNegative() {
        ArmorDef armor = rs.armor("power_suit"); // front 100
        // 0% roll = 0, minus armour clamps to 0
        assertEquals(0, dmg.applyDamage(0, 30, DamageType.ARMOR_PIERCING, armor, Facing.FRONT));
    }

    @Test
    public void seededRngIsDeterministic() {
        Rng a = new SeededRng(42L);
        Rng b = new SeededRng(42L);
        ArmorDef none = rs.armor("none");
        for (int i = 0; i < 100; i++) {
            int da = dmg.rollDamage(a, 30, DamageType.ARMOR_PIERCING, none, Facing.FRONT);
            int db = dmg.rollDamage(b, 30, DamageType.ARMOR_PIERCING, none, Facing.FRONT);
            assertEquals("roll " + i + " must match for equal seeds", da, db);
        }
    }

    @Test
    public void rollStaysWithinZeroToDoublePower() {
        Rng rng = new SeededRng(7L);
        ArmorDef none = rs.armor("none");
        int power = 30;
        for (int i = 0; i < 500; i++) {
            int d = dmg.rollDamage(rng, power, DamageType.ARMOR_PIERCING, none, Facing.FRONT);
            assertTrue(d >= 0 && d <= 2 * power);
        }
    }
}
