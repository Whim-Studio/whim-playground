package com.midnight.core;

/**
 * The kinds of land a tile may hold. Each terrain has a {@link #moveCost()} —
 * the number of daylight hours it costs a lord to ENTER a tile of that type —
 * and an {@link #isPassable()} flag. The northern Mountains of Ithorn
 * ({@link #MOUNTAINS}) and every {@link #LAKE} are impassable; no lord may set
 * foot on them.
 */
public enum Terrain {
    PLAINS(4, true),
    FOREST(8, true),
    MOUNTAINS(999, false),
    SNOW(9, true),
    DOWNS(5, true),
    WASTELAND(10, true),
    CITADEL(4, true),
    KEEP(4, true),
    TOWER(4, true),
    VILLAGE(4, true),
    HENGE(6, true),
    RUINS(6, true),
    LAKE(999, false);

    private final int moveCost;
    private final boolean passable;

    Terrain(int moveCost, boolean passable) {
        this.moveCost = moveCost;
        this.passable = passable;
    }

    /** False for {@link #MOUNTAINS} and {@link #LAKE}; true otherwise. */
    public boolean isPassable() {
        return passable;
    }

    /** Daylight hours needed to enter a tile of this terrain. */
    public int moveCost() {
        return moveCost;
    }
}
