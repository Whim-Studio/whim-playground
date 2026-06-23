package com.tiwas.rpg.engine;

import com.tiwas.rpg.domain.AttributeCode;
import com.tiwas.rpg.domain.Character;

/**
 * Stateless combat damage helpers. All math rounds DOWN; damage floors at 0.
 */
public final class CombatMath {
    private CombatMath() {
    }

    /** Damage to HP = weaponDmgMod + margin + mightBonus - armorRating (min 0); mightBonus = bpp / 20. */
    public static int meleeDamage(Character attacker, int weaponDmgMod, int margin, int armorRating) {
        int mightBonus = attacker.getAttribute(AttributeCode.BPP) / 20;
        return Math.max(0, weaponDmgMod + margin + mightBonus - armorRating);
    }

    /** Damage to HP = weaponDmgMod + margin + agilityBonus - armorRating (min 0); agilityBonus = bsp / 20. */
    public static int rangedDamage(Character attacker, int weaponDmgMod, int margin, int armorRating) {
        int agilityBonus = attacker.getAttribute(AttributeCode.BSP) / 20;
        return Math.max(0, weaponDmgMod + margin + agilityBonus - armorRating);
    }
}
