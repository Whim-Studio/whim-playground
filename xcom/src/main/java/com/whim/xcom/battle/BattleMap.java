package com.whim.xcom.battle;

/**
 * The tactical grid. Pure data + geometry (line-of-sight); no Swing, no rules.
 * Coordinates are {@code (x, y)} with {@code y} increasing "south" (downward on
 * screen). LOS uses a Bresenham supercover-free line that is blocked by any
 * sight-blocking tile strictly between the two endpoints.
 */
public final class BattleMap {

    private final int width;
    private final int height;
    private final Tile[][] tiles;

    public BattleMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new Tile[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = new Tile(Tile.Kind.GRASS);
            }
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public Tile tile(int x, int y) {
        return tiles[x][y];
    }

    public boolean walkable(int x, int y) {
        return inBounds(x, y) && tiles[x][y].walkable();
    }

    /**
     * True if a clear line of sight exists from {@code (x0,y0)} to {@code (x1,y1)}.
     * The endpoints themselves never block; only intermediate tiles do.
     */
    public boolean hasLineOfSight(int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int cx = x0;
        int cy = y0;
        while (true) {
            if (cx == x1 && cy == y1) {
                return true;
            }
            if (!(cx == x0 && cy == y0) && inBounds(cx, cy) && tiles[cx][cy].blocksSight()) {
                return false;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                cx += sx;
            }
            if (e2 < dx) {
                err += dx;
                cy += sy;
            }
        }
    }

    /** Chebyshev (king-move) distance in tiles — the battlescape's notion of range. */
    public static int distance(int x0, int y0, int x1, int y1) {
        return Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
    }
}
