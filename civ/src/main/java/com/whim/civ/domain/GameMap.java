package com.whim.civ.domain;

import java.util.ArrayList;
import java.util.List;

/** Square grid world map. x in [0,width), y in [0,height). */
public final class GameMap {
    private final int width;
    private final int height;
    private final Tile[][] tiles;   // [x][y]

    public GameMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new Tile[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = new Tile(Terrain.OCEAN);
            }
        }
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public Tile getTile(int x, int y) {
        if (!inBounds(x, y)) {
            throw new IndexOutOfBoundsException("Tile out of bounds: (" + x + "," + y + ")");
        }
        return tiles[x][y];
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /**
     * The 21-tile "city work radius" (the fat cross) around (cx,cy), in-bounds only,
     * INCLUDING the center tile. The fat cross is the 5x5 block minus its four corners.
     * Order is stable.
     */
    public List<int[]> cityWorkTiles(int cx, int cy) {
        List<int[]> result = new ArrayList<int[]>();
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                // skip the four corners of the 5x5 block
                if (Math.abs(dx) == 2 && Math.abs(dy) == 2) {
                    continue;
                }
                int x = cx + dx;
                int y = cy + dy;
                if (inBounds(x, y)) {
                    result.add(new int[]{x, y});
                }
            }
        }
        return result;
    }

    /** 8-neighborhood (square adjacency) used for movement & combat adjacency. */
    public List<int[]> neighbors(int x, int y) {
        List<int[]> result = new ArrayList<int[]>();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                int nx = x + dx;
                int ny = y + dy;
                if (inBounds(nx, ny)) {
                    result.add(new int[]{nx, ny});
                }
            }
        }
        return result;
    }
}
