package com.whim.colony.domain;

/**
 * A simple structure or placed item occupying a single {@link MapTile}. Kept
 * intentionally minimal: a type plus a back-reference to the tile it sits on.
 * The engine (Task 2) is responsible for placement/removal semantics.
 */
public final class Building {
    private BuildingType type;
    private MapTile tile; // the tile this building occupies (nullable until placed)

    public Building(BuildingType type) {
        this.type = type;
    }

    public Building(BuildingType type, MapTile tile) {
        this.type = type;
        this.tile = tile;
    }

    public BuildingType getType() {
        return type;
    }

    public void setType(BuildingType type) {
        this.type = type;
    }

    public MapTile getTile() {
        return tile;
    }

    public void setTile(MapTile tile) {
        this.tile = tile;
    }

    /** @return true if this building blocks colonist movement onto its tile. */
    public boolean blocksMovement() {
        return type != null && type.blocksMovement();
    }
}
