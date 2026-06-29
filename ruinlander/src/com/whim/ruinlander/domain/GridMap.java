package com.whim.ruinlander.domain;

/** A rectangular grid of {@link Tile}s. */
public class GridMap {
    private final int width, height;
    private final Tile[][] tiles; // [y][x]
    private Position playerStart = new Position(0, 0);

    public GridMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new Tile[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles[y][x] = new Tile(TerrainType.WASTELAND);
            }
        }
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public boolean inBounds(Position p) {
        return p != null && inBounds(p.x, p.y);
    }

    public Tile getTile(int x, int y) {
        if (!inBounds(x, y)) return null;
        return tiles[y][x];
    }

    public Tile getTile(Position p) {
        return p == null ? null : getTile(p.x, p.y);
    }

    public void setTile(int x, int y, Tile t) {
        if (inBounds(x, y)) {
            tiles[y][x] = t;
        }
    }

    /** Convenience: place (or clear with null) an entity on a tile. */
    public void setEntity(int x, int y, Entity e) {
        Tile t = getTile(x, y);
        if (t != null) {
            t.setEntity(e);
            if (e != null) {
                e.setPosition(new Position(x, y));
            }
        }
    }

    public Position getPlayerStart() { return playerStart; }
    public void setPlayerStart(Position p) { this.playerStart = p; }
}
