package com.whim.b5wars.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Printed weapon definition (immutable). */
public final class Weapon {
    private final String name;
    private final String type;
    private final WeaponArc arc;
    private final int[] rangeBrackets;
    private final int baseToHit;
    private final DamageProfile damage;
    private final int reloadTurns;
    private final Set<WeaponTrait> traits;

    public Weapon(String name, String type, WeaponArc arc, int[] rangeBrackets,
                  int baseToHit, DamageProfile damage, int reloadTurns,
                  Set<WeaponTrait> traits) {
        this.name = name;
        this.type = type;
        this.arc = arc;
        this.rangeBrackets = rangeBrackets == null
                ? new int[0] : rangeBrackets.clone();
        this.baseToHit = baseToHit;
        this.damage = damage;
        this.reloadTurns = reloadTurns;
        EnumSet<WeaponTrait> t = EnumSet.noneOf(WeaponTrait.class);
        if (traits != null) {
            t.addAll(traits);
        }
        this.traits = Collections.unmodifiableSet(t);
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public WeaponArc getArc() {
        return arc;
    }

    /** Ascending max-range per accuracy bracket. */
    public int[] getRangeBrackets() {
        return rangeBrackets.clone();
    }

    /** d20 target before modifiers. */
    public int getBaseToHit() {
        return baseToHit;
    }

    public DamageProfile getDamage() {
        return damage;
    }

    /** 0 = fires every turn. */
    public int getReloadTurns() {
        return reloadTurns;
    }

    public Set<WeaponTrait> getTraits() {
        return traits;
    }

    public boolean has(WeaponTrait t) {
        return traits.contains(t);
    }
}
