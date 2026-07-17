package com.whim.xcom.battle;

/**
 * One battlescape floor cell. A tile can be walked on (or not), can block
 * line-of-sight (a wall/large object), and publishes a movement cost — the base
 * 1994 ground cost is 4 TU (see {@link com.whim.xcom.rules.model.TimeUnitModel}).
 */
public final class Tile {

    /** Terrain kinds — purely cosmetic + cost/LOS behaviour for the slice. */
    public enum Kind {
        GRASS(4, true, false),
        DIRT(4, true, false),
        ROAD(4, true, false),
        ROCK(0, false, true),      // impassable, blocks sight (boulder/wall)
        WALL(0, false, true),      // structure wall
        BUSH(6, true, false),      // passable but slow, does not block LOS
        UFO_HULL(0, false, true),  // outer hull wall
        UFO_FLOOR(4, true, false), // inside a craft
        RUBBLE(4, true, false),    // blasted wall/hull — walkable, no cover
        SCORCHED(4, true, false);  // blasted ground

        final int moveCost;
        final boolean walkable;
        final boolean blocksSight;

        Kind(int moveCost, boolean walkable, boolean blocksSight) {
            this.moveCost = moveCost;
            this.walkable = walkable;
            this.blocksSight = blocksSight;
        }
    }

    private Kind kind;

    public Tile(Kind kind) {
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public boolean walkable() {
        return kind.walkable;
    }

    public boolean blocksSight() {
        return kind.blocksSight;
    }

    public int moveCost() {
        return kind.moveCost;
    }

    /** True if an explosion can destroy this tile's terrain. */
    public boolean destructible() {
        return kind == Kind.WALL || kind == Kind.UFO_HULL
                || kind == Kind.BUSH || kind == Kind.ROCK;
    }

    /**
     * Apply blast destruction: walls/hull become walkable rubble, vegetation and
     * boulders are cleared to scorched ground. Returns true if the tile changed
     * (e.g. a sight-blocker was removed).
     */
    public boolean destroy() {
        if (!destructible()) {
            return false;
        }
        switch (kind) {
            case WALL:
            case UFO_HULL:
                kind = Kind.RUBBLE;
                return true;
            case ROCK:
            case BUSH:
            default:
                kind = Kind.SCORCHED;
                return true;
        }
    }
}
