package com.whim.nextrun.domain;

/** The 2D world grid of tiles. */
public final class GridMap {
    public final int width;
    public final int height;
    private final Tile[][] tiles;

    public GridMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new Tile[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = new Tile();
            }
        }
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public boolean inBounds(Position p) {
        return inBounds(p.x, p.y);
    }

    public Tile at(int x, int y) {
        return tiles[x][y];
    }

    public Tile at(Position p) {
        return tiles[p.x][p.y];
    }

    /** Reveal the tile at (x,y) and its immediate neighbours (fog of war). */
    public void reveal(Position p, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int nx = p.x + dx;
                int ny = p.y + dy;
                if (inBounds(nx, ny) && Math.abs(dx) + Math.abs(dy) <= radius) {
                    tiles[nx][ny].discovered = true;
                }
            }
        }
    }
}
