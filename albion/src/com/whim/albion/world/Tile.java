package com.whim.albion.world;

import com.whim.albion.api.Enums.TileType;
import com.whim.albion.api.Views.TileView;

/**
 * One immutable map cell. {@code walkable} drives collision; {@code blocksSight}
 * is consumed by the UI's first-person renderer to decide where to draw wall
 * quads. {@code decorKey} is a procedural-art hint ("" for none).
 */
public final class Tile implements TileView {

    private final TileType type;
    private final boolean walkable;
    private final boolean blocksSight;
    private final String decorKey;

    public Tile(TileType type, boolean walkable, boolean blocksSight, String decorKey) {
        this.type = type;
        this.walkable = walkable;
        this.blocksSight = blocksSight;
        this.decorKey = decorKey == null ? "" : decorKey;
    }

    /** Convenience for the common cases, with sensible collision defaults per type. */
    public static Tile of(TileType type) {
        switch (type) {
            case WALL:
            case OBSTACLE:
                return new Tile(type, false, true, "");
            case WATER:
            case VOID:
                return new Tile(type, false, false, "");
            case DOOR:
                return new Tile(type, true, true, "");
            case STAIRS:
                return new Tile(type, true, false, "");
            default: // GRASS, PATH, FLOOR
                return new Tile(type, true, false, "");
        }
    }

    public Tile withDecor(String decorKey) {
        return new Tile(type, walkable, blocksSight, decorKey);
    }

    public Tile asBlocked() {
        return new Tile(type, false, blocksSight, decorKey);
    }

    @Override public TileType type() { return type; }
    @Override public boolean walkable() { return walkable; }
    @Override public boolean blocksSight() { return blocksSight; }
    @Override public String decorKey() { return decorKey; }
}
