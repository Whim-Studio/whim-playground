package com.railroad.model;

/**
 * A single map cell. Holds its grid position and terrain class. Kept deliberately
 * small; later phases can attach ownership, resources or improvements here.
 */
public final class Tile {

    private final int x;
    private final int y;
    private TerrainType terrain;

    public Tile(int x, int y, TerrainType terrain) {
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

    public GridPoint position() {
        return new GridPoint(x, y);
    }
}
