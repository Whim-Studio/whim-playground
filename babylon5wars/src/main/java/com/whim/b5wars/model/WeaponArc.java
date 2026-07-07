package com.whim.b5wars.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** The set of Facings a weapon mount can fire into. */
public final class WeaponArc {
    private final Set<Facing> facings;

    public WeaponArc(Set<Facing> facings) {
        EnumSet<Facing> copy = EnumSet.noneOf(Facing.class);
        if (facings != null) {
            copy.addAll(facings);
        }
        this.facings = Collections.unmodifiableSet(copy);
    }

    public boolean contains(Facing f) {
        return facings.contains(f);
    }

    public Set<Facing> facings() {
        return facings;
    }

    public static WeaponArc of(Facing... f) {
        EnumSet<Facing> set = EnumSet.noneOf(Facing.class);
        if (f != null) {
            for (Facing facing : f) {
                set.add(facing);
            }
        }
        return new WeaponArc(set);
    }

    /** All 6 hexsides. */
    public static WeaponArc all() {
        return new WeaponArc(EnumSet.allOf(Facing.class));
    }

    /** The forward firing arc: {FL, F, FR}. */
    public static WeaponArc forward() {
        return of(Facing.FL, Facing.F, Facing.FR);
    }
}
