package com.railroad.model;

/**
 * A rectangular grid of {@link Tile}s addressed by (x, y). This is the terrain
 * layer of the world; towns and track are stored separately so the grid stays a
 * pure spatial container.
 */
public final class TileGrid {

    private final int width;
    private final int height;
    private final Tile[][] tiles; // [x][y]

    public TileGrid(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new Tile[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = new Tile(x, y, TerrainType.CLEAR);
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public boolean inBounds(GridPoint p) {
        return inBounds(p.x, p.y);
    }

    /** Tile at (x, y). Caller must ensure the coordinate is in bounds. */
    public Tile tileAt(int x, int y) {
        return tiles[x][y];
    }

    public Tile tileAt(GridPoint p) {
        return tiles[p.x][p.y];
    }

    public TerrainType terrainAt(int x, int y) {
        return tiles[x][y].getTerrain();
    }
}
