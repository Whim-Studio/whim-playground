package com.whim.startrek.domain;

/**
 * Terrain a {@link GridCell} can hold. Behaviour is data-driven so the engine and
 * UI read it uniformly: {@link #destroysAssets()} marks lethal terrain, and the
 * hull-damage range gives a percent-of-max-hull band applied to fleets that enter.
 */
public enum MapObjectType {
    // destroysAssets, hullDamageMinPct, hullDamageMaxPct
    EMPTY            (false, 0,  0),
    SOLAR_SYSTEM     (false, 0,  0),
    NEBULA           (false, 0,  0),   // blocks sensors / cloak detection
    ENERGY_STORM     (false, 5,  15),
    SUPERNOVA        (true,  0,  0),
    STABLE_WORMHOLE  (false, 0,  0),   // safe teleport between linked cells
    UNSTABLE_WORMHOLE(false, 25, 50),  // teleport + 25-50% hull damage
    BLACK_HOLE       (true,  0,  0),   // destroys any fleet entering
    SUPER_BLACK_HOLE (true,  0,  0);   // destroys fleet + may collapse neighbor cells

    private final boolean destroysAssets;
    private final int hullDamageMinPct;
    private final int hullDamageMaxPct;

    MapObjectType(boolean destroysAssets, int hullDamageMinPct, int hullDamageMaxPct) {
        this.destroysAssets = destroysAssets;
        this.hullDamageMinPct = hullDamageMinPct;
        this.hullDamageMaxPct = hullDamageMaxPct;
    }

    public boolean destroysAssets() {
        return destroysAssets;
    }

    public int getHullDamageMinPct() {
        return hullDamageMinPct;
    }

    public int getHullDamageMaxPct() {
        return hullDamageMaxPct;
    }
}
