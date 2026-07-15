package com.whim.ruinlander.domain;

/** A single map cell: terrain, an optional occupying entity, and a discovered flag. */
public class Tile {
    private TerrainType terrain;
    private Entity entity;     // nullable
    private boolean discovered;

    public Tile(TerrainType terrain) {
        this.terrain = terrain;
    }

    public TerrainType getTerrain() { return terrain; }
    public void setTerrain(TerrainType terrain) { this.terrain = terrain; }

    public Entity getEntity() { return entity; }
    public void setEntity(Entity entity) { this.entity = entity; }
    public boolean hasEntity() { return entity != null; }

    public boolean isDiscovered() { return discovered; }
    public void setDiscovered(boolean discovered) { this.discovered = discovered; }

    /** Whether the player can walk onto this tile (terrain-wise). */
    public boolean isPassable() {
        return terrain != TerrainType.WATER;
    }
}
