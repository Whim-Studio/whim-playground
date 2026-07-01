package com.whim.colony.domain;

/**
 * The natural ground type of a {@link MapTile}. The {@code walkable} flag is the
 * terrain's intrinsic traversability; a tile's final walkability also depends on
 * any {@link Building} sitting on it (see {@link MapTile#isWalkable()}).
 */
public enum TerrainType {
    GRASS(true),
    DIRT(true),
    ROCK(false),
    WATER(false),
    WALL(false);

    private final boolean walkable;

    TerrainType(boolean walkable) {
        this.walkable = walkable;
    }

    /** @return true if a colonist can, in principle, stand on this terrain. */
    public boolean isWalkable() {
        return walkable;
    }
}
