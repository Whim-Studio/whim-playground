package com.whim.powermonger.domain;

import com.whim.powermonger.api.Enums.TerrainType;

/**
 * A rectangular grid of {@link Tile}s (target 48x48). Passive container with
 * bounds-safe accessors and a deforest helper.
 */
public final class MapGrid {

    private final int width;
    private final int height;
    private final int maxElevation;
    private final Tile[][] tiles; // [x][y]

    public MapGrid(int width, int height, int maxElevation) {
        this.width = width;
        this.height = height;
        this.maxElevation = maxElevation;
        this.tiles = new Tile[width][height];
    }

    public int width() { return width; }
    public int height() { return height; }
    public int maxElevation() { return maxElevation; }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    /** The tile at (x,y), or null if out of bounds. */
    public Tile tile(int x, int y) {
        if (!inBounds(x, y)) return null;
        return tiles[x][y];
    }

    public void setTile(int x, int y, Tile t) {
        if (inBounds(x, y)) tiles[x][y] = t;
    }

    /**
     * Turn a FOREST tile into GRASS, clearing its trees. Returns true if a
     * forest was actually cleared. Weather/movement side-effects are the
     * engine's job; the domain only records the terrain change.
     */
    public boolean deforest(int x, int y) {
        Tile t = tile(x, y);
        if (t == null) return false;
        if (t.terrain() != TerrainType.FOREST && !t.hasTrees()) return false;
        t.setTerrain(TerrainType.GRASS);
        t.setTrees(false);
        return true;
    }
}
