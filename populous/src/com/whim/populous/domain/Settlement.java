package com.whim.populous.domain;

import com.whim.populous.api.Enums.Allegiance;
import com.whim.populous.api.Enums.SettlementType;

/**
 * A dwelling built by a follower on a flat plateau. Its {@link #type()} tier is
 * decided by the plateau size at build time (see {@link SettlementRules}) and can
 * be upgraded later when the surrounding land is flattened further, or toppled
 * back to NONE by disasters.
 *
 * <p>The settlement's tier drives two engine-side rates: how quickly it breeds
 * new followers and how much mana it contributes each tick.
 */
public final class Settlement {

    private final Tile home;
    private Allegiance owner;
    private SettlementType type;
    private int level;          // small index within tier for the renderer

    /** Breeding accumulator (engine ticks up; spawns a follower on overflow). */
    private double breedProgress;

    public Settlement(Tile home, Allegiance owner, SettlementType type, int level) {
        this.home = home;
        this.owner = owner;
        this.type = type;
        this.level = level;
    }

    public Tile home() { return home; }
    public Allegiance owner() { return owner; }
    public SettlementType type() { return type; }
    public int level() { return level; }

    public int populationWeight() { return SettlementRules.populationWeight(type); }
    public int manaWeight() { return SettlementRules.manaWeight(type); }

    // ---- engine mutation ----------------------------------------------------

    public void setOwner(Allegiance a) { this.owner = a; }

    /** Re-tier this settlement from a freshly measured flat-plateau size. */
    public void retier(int flatTiles) {
        this.type = SettlementRules.tierFor(flatTiles);
        this.level = SettlementRules.levelWithinTier(flatTiles);
    }

    public void setType(SettlementType type) { this.type = type; }
    public void setLevel(int level) { this.level = level; }

    public double breedProgress() { return breedProgress; }
    public void addBreedProgress(double delta) { this.breedProgress += delta; }
    public void resetBreedProgress() { this.breedProgress = 0.0; }
}
