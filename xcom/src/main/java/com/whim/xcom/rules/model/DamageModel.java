package com.whim.xcom.rules.model;

import com.whim.xcom.model.DamageType;
import com.whim.xcom.rng.Rng;
import com.whim.xcom.rules.def.ArmorDef;

/**
 * Rolls inflicted damage. The 1994 rule: roll a uniform {@code 0%..200%} of the
 * weapon's nominal power (mean = power), apply the target's armour on the struck
 * facing and the armour's per-type resistance multiplier, and the remainder is
 * damage to health (or stun/energy for those types).
 */
public interface DamageModel {

    /** Which facing of the target was struck (selects the armour value). */
    enum Facing { FRONT, SIDE, REAR, UNDER }

    /**
     * @param rng    deterministic source for the 0..200% roll
     * @param power  weapon nominal power
     * @param type   damage type
     * @param armor  target armour def (may be {@code null} for unarmoured)
     * @param facing struck facing
     * @return final health/stun damage, never negative
     */
    int rollDamage(Rng rng, int power, DamageType type, ArmorDef armor, Facing facing);

    /**
     * Deterministic variant used for tests and previews: caller supplies the
     * {@code rollPercent} (0..200) instead of drawing from an {@link Rng}.
     */
    int applyDamage(int rollPercent, int power, DamageType type, ArmorDef armor, Facing facing);
}
