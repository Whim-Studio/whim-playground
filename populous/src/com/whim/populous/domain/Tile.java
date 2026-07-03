package com.whim.populous.domain;

import com.whim.populous.api.Enums.Allegiance;
import com.whim.populous.api.Enums.SettlementType;
import com.whim.populous.api.Enums.TerrainType;
import com.whim.populous.api.Views.TileView;

/**
 * One landscape cell. Implements the read-only {@link TileView} the UI renders
 * through, and adds the mutation surface the engine drives.
 *
 * <p>Terrain is <em>derived</em> from {@link #elevation()} via {@link TerrainRules}
 * against the owning {@link MapGrid}'s live sea level, except when a transient
 * override (SWAMP from the Swamp power, LAVA from a Volcano) is active. Overrides
 * carry a time-to-live in ticks so the engine can let them decay.
 */
public final class Tile implements TileView {

    private final MapGrid map;   // back-reference for the live sea level
    private final int col;
    private final int row;

    private int elevation;
    private Allegiance owner = Allegiance.NEUTRAL;
    private Settlement settlement;         // null when empty

    private TerrainType override;          // null => derive from elevation
    private int overrideTtl;               // remaining ticks for the override

    Tile(MapGrid map, int col, int row, int elevation) {
        this.map = map;
        this.col = col;
        this.row = row;
        this.elevation = elevation;
    }

    // ---- TileView (read-only projection) -----------------------------------

    @Override public int col() { return col; }
    @Override public int row() { return row; }
    @Override public int elevation() { return elevation; }

    @Override
    public TerrainType terrain() {
        if (override != null) {
            return override;
        }
        return TerrainRules.terrainFor(elevation, map.seaLevel());
    }

    @Override
    public Allegiance owner() {
        return owner;
    }

    @Override
    public SettlementType settlement() {
        return settlement == null ? SettlementType.NONE : settlement.type();
    }

    @Override
    public int settlementLevel() {
        return settlement == null ? 0 : settlement.level();
    }

    // ---- Engine mutation surface -------------------------------------------

    public void setElevation(int e) { this.elevation = e; }
    public void addElevation(int delta) { this.elevation += delta; }

    public void setOwner(Allegiance a) { this.owner = a; }

    /** The concrete settlement on this tile, or null. */
    public Settlement settlementRef() { return settlement; }

    /** Attach/replace a settlement; also claims the tile's territory ownership. */
    public void setSettlement(Settlement s) {
        this.settlement = s;
        if (s != null) {
            this.owner = s.owner();
        }
    }

    /** Remove any settlement (e.g. toppled by an earthquake) but keep ownership. */
    public void clearSettlement() {
        this.settlement = null;
    }

    /** Stamp a transient terrain override (SWAMP/LAVA) for {@code ttl} ticks. */
    public void setTransient(TerrainType t, int ttl) {
        this.override = t;
        this.overrideTtl = ttl;
    }

    /** True while a transient override is in effect. */
    public boolean hasTransient() {
        return override != null;
    }

    public TerrainType transientTerrain() { return override; }

    /**
     * Age any transient override by one tick, clearing it when it expires.
     * @return true if an override cleared on this call.
     */
    public boolean ageTransient() {
        if (override == null) {
            return false;
        }
        if (--overrideTtl <= 0) {
            override = null;
            overrideTtl = 0;
            return true;
        }
        return false;
    }

    // ---- convenience derived queries ---------------------------------------

    public boolean isWater() { return TerrainRules.isWater(elevation, map.seaLevel()); }
    public boolean isDryLand() { return TerrainRules.isDryLand(elevation, map.seaLevel()); }
    public boolean hasSettlement() { return settlement != null; }
}
