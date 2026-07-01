package com.whim.colony.domain;

/**
 * The 2D grid of {@link MapTile}s that makes up the colony world. Tiles are
 * created up front as GRASS; the engine (Task 2) is expected to sculpt terrain
 * during map generation.
 */
public final class ColonyMap {
    private final int width;
    private final int height;
    private final MapTile[][] tiles; // indexed [x][y]

    public ColonyMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new MapTile[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = new MapTile(x, y, TerrainType.GRASS);
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /** @return true if (x,y) is a valid coordinate inside the grid. */
    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    /**
     * @return the tile at (x,y), or {@code null} if the coordinate is out of
     * bounds. Callers should guard with {@link #inBounds(int, int)}.
     */
    public MapTile getTile(int x, int y) {
        if (!inBounds(x, y)) {
            return null;
        }
        return tiles[x][y];
    }
}
