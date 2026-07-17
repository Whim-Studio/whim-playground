package com.whim.xcom.battle;

import com.whim.xcom.model.DamageType;
import com.whim.xcom.rules.def.ArmorDef;

/**
 * Small helpers to synthesise {@link ArmorDef} values the tactical layer needs
 * that aren't wearable armour items — chiefly an alien race's innate, uniform
 * armour. Keeps the battle package from depending on the data-pack classes.
 */
public final class Armors {

    private Armors() {
    }

    /** A uniform armour value on every facing, normal resistance to all types. */
    public static ArmorDef uniform(final String id, final int value) {
        return new ArmorDef() {
            @Override public String id() { return id; }
            @Override public String name() { return id; }
            @Override public int front() { return value; }
            @Override public int side() { return value; }
            @Override public int rear() { return value; }
            @Override public int under() { return value; }
            @Override public double resistance(DamageType type) { return 1.0; }
            @Override public int defaultArmor() { return value; }
        };
    }
}
