package com.whim.xcom.rules.def;

import com.whim.xcom.model.DamageType;

/**
 * Personal armour. 1994 armour has four directional values (front/side/rear/
 * under) plus per-damage-type resistance multipliers.
 */
public interface ArmorDef extends GameDef {

    int front();

    int side();

    int rear();

    int under();

    /** Resistance multiplier vs a damage type (1.0 = normal, &lt;1 tougher, &gt;1 weaker). */
    double resistance(DamageType type);

    /** Extra TU/stamina or reaction bonuses could hang here later; front armour by default. */
    int defaultArmor();
}
