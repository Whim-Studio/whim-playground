package com.whim.necromunda.model.board;

/**
 * A single board cell: its terrain, an integer height/level (for multi-level
 * underhive terrain and line-of-sight), and a cover value (defaulting to the
 * terrain's, but overridable per tile by the board author).
 */
public final class Tile {

    private TerrainType terrain;
    private int height;
    private Cover cover;

    public Tile(TerrainType terrain, int height) {
        this.terrain = terrain;
        this.height = height;
        this.cover = terrain.defaultCover();
    }

    public Tile(TerrainType terrain) {
        this(terrain, 0);
    }

    public TerrainType terrain() { return terrain; }
    public void setTerrain(TerrainType terrain) {
        this.terrain = terrain;
        this.cover = terrain.defaultCover();
    }

    public int height() { return height; }
    public void setHeight(int height) { this.height = height; }

    public Cover cover() { return cover; }
    public void setCover(Cover cover) { this.cover = cover; }

    public boolean blocksMovement() { return terrain.blocksMovement(); }
    public boolean blocksSight() { return terrain.blocksSight(); }
}
