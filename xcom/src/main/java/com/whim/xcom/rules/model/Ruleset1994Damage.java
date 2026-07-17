package com.whim.xcom.rules.model;

import com.whim.xcom.model.DamageType;
import com.whim.xcom.rng.Rng;
import com.whim.xcom.rules.def.ArmorDef;

/**
 * 1994 damage model: {@code damage = power × roll(0..200)/100}, then subtract the
 * struck facing's armour and apply the armour's per-type resistance multiplier.
 * Source: UFOpaedia "Damage".
 */
public final class Ruleset1994Damage implements DamageModel {

    @Override
    public int rollDamage(Rng rng, int power, DamageType type, ArmorDef armor, Facing facing) {
        return applyDamage(rng.rollPercent0to200(), power, type, armor, facing);
    }

    @Override
    public int applyDamage(int rollPercent, int power, DamageType type, ArmorDef armor, Facing facing) {
        if (rollPercent < 0) {
            rollPercent = 0;
        }
        if (rollPercent > 200) {
            rollPercent = 200;
        }
        int rolled = (int) Math.floor(power * (double) rollPercent / 100.0);

        int armorValue = 0;
        double resist = 1.0;
        if (armor != null) {
            armorValue = facingArmor(armor, facing);
            resist = armor.resistance(type);
        }
        // Armour is subtracted first, then the type resistance scales what gets through.
        int afterArmor = rolled - armorValue;
        if (afterArmor <= 0) {
            return 0;
        }
        int result = (int) Math.floor(afterArmor * resist);
        return Math.max(result, 0);
    }

    private static int facingArmor(ArmorDef armor, Facing facing) {
        switch (facing) {
            case FRONT: return armor.front();
            case SIDE:  return armor.side();
            case REAR:  return armor.rear();
            case UNDER: return armor.under();
            default:    return armor.front();
        }
    }
}
