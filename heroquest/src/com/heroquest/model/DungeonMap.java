package com.heroquest.model;

/** The 2D dungeon grid: walls, floors, doors, furniture, traps and Fog of War state. */
public final class DungeonMap {
    private final int width;
    private final int height;
    private final Tile[][] tiles; // [y][x]

    public DungeonMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new Tile[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles[y][x] = new Tile(TileType.WALL, -1);
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

    public boolean inBounds(Point p) {
        return inBounds(p.x, p.y);
    }

    public Tile tileAt(int x, int y) {
        return tiles[y][x];
    }

    public Tile tileAt(Point p) {
        return tiles[p.y][p.x];
    }

    public boolean isWalkable(Point p) {
        return inBounds(p) && tileAt(p).isWalkable();
    }

    /** True if a straight ray can pass through this tile (open floor / open door). */
    public boolean isTransparent(Point p) {
        if (!inBounds(p)) {
            return false;
        }
        TileType t = tileAt(p).getType();
        return t == TileType.FLOOR || t == TileType.DOOR_OPEN;
    }
}
