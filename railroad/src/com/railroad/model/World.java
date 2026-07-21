package com.railroad.model;

import java.util.Collections;
import java.util.List;

/**
 * The static world: the terrain {@link TileGrid} plus the list of towns. This is
 * the single source of truth for geography. It is produced once by the map
 * generator and thereafter only read.
 */
public final class World {

    private final TileGrid grid;
    private final List<Town> towns;
    private final long seed;

    public World(TileGrid grid, List<Town> towns, long seed) {
        this.grid = grid;
        this.towns = towns;
        this.seed = seed;
    }

    public TileGrid getGrid() {
        return grid;
    }

    /** Unmodifiable town list. */
    public List<Town> getTowns() {
        return Collections.unmodifiableList(towns);
    }

    public Town townAt(GridPoint p) {
        for (Town t : towns) {
            if (t.getPosition().equals(p)) {
                return t;
            }
        }
        return null;
    }

    /** Seed used to generate this world (for reproducibility / debugging). */
    public long getSeed() {
        return seed;
    }
}
