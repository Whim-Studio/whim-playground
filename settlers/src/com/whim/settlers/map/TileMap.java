package com.whim.settlers.map;

/**
 * A rectangular grid of terrain tiles. Phase 0 provides a flat, mostly-grass
 * placeholder map so there is something to render, pan, and zoom over; Phase 1
 * replaces {@link #flat} with real hand-built maps and a seeded generator.
 */
public final class TileMap {

    private final int width;
    private final int height;
    private final TerrainType[] tiles;

    public TileMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new TerrainType[width * height];
        java.util.Arrays.fill(tiles, TerrainType.GRASS);
    }

    public int width()  { return width; }
    public int height() { return height; }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public TerrainType get(int x, int y) {
        return tiles[y * width + x];
    }

    public void set(int x, int y, TerrainType type) {
        tiles[y * width + x] = type;
    }

    /**
     * Build a simple placeholder landscape: a grass field with a small lake and
     * a scattering of forest and mountain tiles, laid out deterministically from
     * a seed so the scaffold looks like terrain rather than a blank sheet.
     */
    public static TileMap flat(int width, int height, long seed) {
        TileMap m = new TileMap(width, height);
        java.util.Random rng = new java.util.Random(seed);
        // A rough lake in one quadrant.
        int lakeCx = width / 4, lakeCy = height / 3, lakeR = Math.min(width, height) / 8;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double d = Math.hypot(x - lakeCx, y - lakeCy);
                if (d < lakeR) {
                    m.set(x, y, TerrainType.WATER);
                } else if (rng.nextDouble() < 0.06) {
                    m.set(x, y, TerrainType.FOREST);
                } else if (rng.nextDouble() < 0.015) {
                    m.set(x, y, mountain(rng));
                }
            }
        }
        return m;
    }

    private static TerrainType mountain(java.util.Random rng) {
        switch (rng.nextInt(4)) {
            case 0:  return TerrainType.MOUNTAIN_COAL;
            case 1:  return TerrainType.MOUNTAIN_IRON;
            case 2:  return TerrainType.MOUNTAIN_GOLD;
            default: return TerrainType.MOUNTAIN_STONE;
        }
    }
}
