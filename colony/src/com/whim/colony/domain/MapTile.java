package com.whim.colony.domain;

/**
 * One cell of the {@link ColonyMap} grid. A tile knows its coordinates, its
 * {@link TerrainType}, and optionally a {@link Building} placed on it. Final
 * traversability is derived from both terrain and building.
 */
public final class MapTile {
    private final int x;
    private final int y;
    private TerrainType terrain;
    private Building building; // nullable

    public MapTile(int x, int y, TerrainType terrain) {
        this.x = x;
        this.y = y;
        this.terrain = terrain;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public TerrainType getTerrain() {
        return terrain;
    }

    public void setTerrain(TerrainType terrain) {
        this.terrain = terrain;
    }

    public Building getBuilding() {
        return building;
    }

    public void setBuilding(Building building) {
        this.building = building;
    }

    public boolean hasBuilding() {
        return building != null;
    }

    /**
     * @return true if a colonist may stand on this tile: the terrain must be
     * walkable and any building on it must not block movement.
     */
    public boolean isWalkable() {
        if (terrain == null || !terrain.isWalkable()) {
            return false;
        }
        return building == null || !building.blocksMovement();
    }
}
