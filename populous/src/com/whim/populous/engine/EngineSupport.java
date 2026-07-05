package com.whim.populous.engine;

import com.whim.populous.api.Enums.Allegiance;
import com.whim.populous.api.Enums.TerrainType;
import com.whim.populous.domain.Follower;
import com.whim.populous.domain.GameState;
import com.whim.populous.domain.MapGrid;
import com.whim.populous.domain.Settlement;
import com.whim.populous.domain.Tile;

import java.util.List;

/**
 * Small bridge helpers between the engine and Task 1's domain. The domain has
 * no global settlement list (settlements live on tiles) and returns {@code null}
 * tiles out of bounds, so these helpers centralise the safe access patterns the
 * engine subsystems rely on.
 */
final class EngineSupport {

    /** Deep-water sentinel returned for out-of-bounds elevation queries. */
    static final int OOB_ELEVATION = Integer.MIN_VALUE / 4;

    private EngineSupport() { }

    /** Elevation at a cell, or a deep-water sentinel if out of bounds. */
    static int elevationAt(MapGrid map, int col, int row) {
        Tile t = map.tile(col, row);
        return t == null ? OOB_ELEVATION : t.elevation();
    }

    /** The concrete settlement standing on a cell, or null. */
    static Settlement settlementAt(MapGrid map, int col, int row) {
        Tile t = map.tile(col, row);
        return t == null ? null : t.settlementRef();
    }

    /** Owner of a cell (NEUTRAL if out of bounds). */
    static Allegiance ownerAt(MapGrid map, int col, int row) {
        Tile t = map.tile(col, row);
        return t == null ? Allegiance.NEUTRAL : t.owner();
    }

    /** Terrain class of a cell (WATER if out of bounds). */
    static TerrainType terrainAt(MapGrid map, int col, int row) {
        Tile t = map.tile(col, row);
        return t == null ? TerrainType.WATER : t.terrain();
    }

    /** Count of living walkers on a side — authoritative mid-tick (after births). */
    static int livePopulation(GameState gs, Allegiance side) {
        List<Follower> list = gs.followerList();
        int n = 0;
        for (int i = 0; i < list.size(); i++) {
            Follower f = list.get(i);
            if (f.alive() && f.allegiance() == side) {
                n++;
            }
        }
        return n;
    }
}
