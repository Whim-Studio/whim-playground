package com.whim.oggalaxy.model;

import com.whim.oggalaxy.api.Cost;
import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.api.Views;

import java.io.Serializable;

/**
 * Holds a planet's stored metal/crystal/deuterium plus a cached snapshot of production,
 * capacity and energy figures that the engine recomputes each tick from the planet's
 * buildings (via {@code Formulas}). Implements {@link Views.ResourceView}.
 *
 * Dark matter is an empire-wide currency; the engine mirrors the owning empire's balance
 * into {@link #darkMatter} so the UI's resource bar can read it through this view.
 */
public final class ResourceStore implements Views.ResourceView, Serializable {

    private static final long serialVersionUID = 1L;

    // stored amounts
    public double metal;
    public double crystal;
    public double deuterium;
    public double darkMatter; // mirror of the owning empire's balance (for display)

    // cached per-tick figures (recomputed by the engine)
    public double capMetal = 100000;
    public double capCrystal = 100000;
    public double capDeut = 100000;
    public double prodMetal;
    public double prodCrystal;
    public double prodDeut;
    public double eProduced;
    public double eConsumed;

    public ResourceStore() {
    }

    public ResourceStore(double metal, double crystal, double deuterium) {
        this.metal = metal;
        this.crystal = crystal;
        this.deuterium = deuterium;
    }

    /** True if this store can cover the given cost's metal/crystal/deuterium. */
    public boolean canAfford(Cost c) {
        return metal >= c.metal && crystal >= c.crystal && deuterium >= c.deuterium;
    }

    /** Deduct a cost's metal/crystal/deuterium (caller must check {@link #canAfford}). */
    public void spend(Cost c) {
        metal -= c.metal;
        crystal -= c.crystal;
        deuterium -= c.deuterium;
    }

    /** Add resources, clamped to capacity for m/c/d. */
    public void add(double m, double c, double d) {
        metal = Math.min(capMetal, metal + m);
        crystal = Math.min(capCrystal, crystal + c);
        deuterium = Math.min(capDeut, deuterium + d);
    }

    public void addCost(Cost c) {
        add(c.metal, c.crystal, c.deuterium);
    }

    @Override
    public double amount(Ids.ResourceType type) {
        switch (type) {
            case METAL: return metal;
            case CRYSTAL: return crystal;
            case DEUTERIUM: return deuterium;
            case DARK_MATTER: return darkMatter;
            case ENERGY: return eProduced - eConsumed;
            default: return 0;
        }
    }

    @Override
    public double capacity(Ids.ResourceType type) {
        switch (type) {
            case METAL: return capMetal;
            case CRYSTAL: return capCrystal;
            case DEUTERIUM: return capDeut;
            default: return Double.POSITIVE_INFINITY;
        }
    }

    @Override
    public double productionPerTick(Ids.ResourceType type) {
        switch (type) {
            case METAL: return prodMetal;
            case CRYSTAL: return prodCrystal;
            case DEUTERIUM: return prodDeut;
            default: return 0;
        }
    }

    @Override public double energyProduced() { return eProduced; }
    @Override public double energyConsumed() { return eConsumed; }
    @Override public double energyRatio() {
        return eConsumed <= 0 ? 1.0 : Math.min(1.0, eProduced / eConsumed);
    }
}
