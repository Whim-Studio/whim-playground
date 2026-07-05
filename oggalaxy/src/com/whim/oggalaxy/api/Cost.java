package com.whim.oggalaxy.api;

import java.io.Serializable;

/**
 * Immutable resource cost / amount triple (metal, crystal, deuterium) with an
 * optional energy figure. Used for building/tech/ship costs and for fleet cargo.
 *
 * Values are stored as {@code double} so production math and partial amounts stay
 * exact enough; UI rounds for display.
 */
public final class Cost implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final Cost ZERO = new Cost(0, 0, 0, 0);

    public final double metal;
    public final double crystal;
    public final double deuterium;
    public final double energy;

    public Cost(double metal, double crystal, double deuterium) {
        this(metal, crystal, deuterium, 0);
    }

    public Cost(double metal, double crystal, double deuterium, double energy) {
        this.metal = metal;
        this.crystal = crystal;
        this.deuterium = deuterium;
        this.energy = energy;
    }

    public double get(Ids.ResourceType type) {
        switch (type) {
            case METAL: return metal;
            case CRYSTAL: return crystal;
            case DEUTERIUM: return deuterium;
            case ENERGY: return energy;
            default: return 0;
        }
    }

    public Cost plus(Cost o) {
        return new Cost(metal + o.metal, crystal + o.crystal, deuterium + o.deuterium, energy + o.energy);
    }

    public Cost scale(double f) {
        return new Cost(metal * f, crystal * f, deuterium * f, energy * f);
    }

    /** Total "structure points" = metal + crystal + deuterium, used for combat/debris/score math. */
    public double structurePoints() {
        return metal + crystal + deuterium;
    }

    @Override
    public String toString() {
        return "Cost[m=" + (long) metal + ", c=" + (long) crystal + ", d=" + (long) deuterium
                + (energy != 0 ? ", e=" + (long) energy : "") + "]";
    }
}
