package com.whim.populous.domain;

import com.whim.populous.api.Enums.TerrainType;

/**
 * Single source of truth for elevation -&gt; {@link TerrainType} derivation.
 *
 * <p>Modelled on Populous (1989)'s eight base land levels between the water
 * table and the highest peaks. Land is only meaningfully "buildable" once it is
 * flat and at or above sea level; the classic game shows beach (sand) at the
 * waterline, lush grass just above it, then rockier ground as height climbs.
 *
 * <p>Thresholds (relative to {@code seaLevel}, matching the CONTRACT):
 * <pre>
 *   e &lt; seaLevel-1  -&gt; WATER    (deep sea, drowns walkers)
 *   e == seaLevel-1  -&gt; SHALLOW  (surf, still drowns walkers)
 *   e == seaLevel    -&gt; SAND     (coast/beach, buildable but poor)
 *   e in +1..+2      -&gt; GRASS    (prime flat building land)
 *   e in +3..+4      -&gt; HILL
 *   e in +5..+6      -&gt; MOUNTAIN
 *   e &gt;= +7         -&gt; ROCK     (bare peaks)
 * </pre>
 * SWAMP and LAVA are transient overrides written directly onto a {@link Tile}
 * by divine powers (Swamp, Volcano) and are not derived from elevation.
 */
public final class TerrainRules {

    private TerrainRules() { }

    /** Deep water below this relative depth. */
    public static final int SHALLOW_DEPTH = 1;   // seaLevel-1 is SHALLOW

    /** Height (above sea) where GRASS gives way to HILL. */
    public static final int GRASS_TOP = 2;
    /** Height where HILL gives way to MOUNTAIN. */
    public static final int HILL_TOP = 4;
    /** Height where MOUNTAIN gives way to bare ROCK. */
    public static final int MOUNTAIN_TOP = 6;

    /**
     * Derive the base terrain purely from elevation. Transient overrides
     * (SWAMP/LAVA) are applied by {@link Tile#terrain()}, not here.
     */
    public static TerrainType terrainFor(int elevation, int seaLevel) {
        int h = elevation - seaLevel; // height above sea in whole steps
        if (h < -SHALLOW_DEPTH) {
            return TerrainType.WATER;
        }
        if (h < 0) {
            return TerrainType.SHALLOW;
        }
        if (h == 0) {
            return TerrainType.SAND;
        }
        if (h <= GRASS_TOP) {
            return TerrainType.GRASS;
        }
        if (h <= HILL_TOP) {
            return TerrainType.HILL;
        }
        if (h <= MOUNTAIN_TOP) {
            return TerrainType.MOUNTAIN;
        }
        return TerrainType.ROCK;
    }

    /** True if the tile height is land a walker can stand on (SAND and up). */
    public static boolean isDryLand(int elevation, int seaLevel) {
        return elevation >= seaLevel;
    }

    /** True if standing here drowns a walker (deep or shallow water). */
    public static boolean isWater(int elevation, int seaLevel) {
        return elevation < seaLevel;
    }

    /** True if this base terrain is flat, dry and good enough to build on. */
    public static boolean isBuildable(int elevation, int seaLevel) {
        return elevation >= seaLevel;
    }
}
