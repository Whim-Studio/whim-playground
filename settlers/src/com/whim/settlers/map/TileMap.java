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
    /** Remaining mineable yield per tile; -1 = not yet initialised (lazy). */
    private final int[] resource;

    public TileMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new TerrainType[width * height];
        java.util.Arrays.fill(tiles, TerrainType.GRASS);
        this.resource = new int[width * height];
        java.util.Arrays.fill(this.resource, -1);
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
        int i = y * width + x;
        tiles[i] = type;
        resource[i] = -1; // terrain change resets the deposit for the new terrain
    }

    /**
     * Remaining mineable yield at a tile (deterministic). Mountains and stone
     * carry a fixed starting reserve; everything else is 0. Computed lazily and
     * cached so a fresh map needs no extra generation pass.
     */
    public int resourceAt(int x, int y) {
        if (!inBounds(x, y)) return 0;
        int i = y * width + x;
        if (resource[i] == -1) resource[i] = defaultYield(tiles[i]);
        return resource[i];
    }

    /**
     * Extract one unit from a tile's deposit. Returns true if a unit was taken
     * (yield &gt; 0), false if the deposit is exhausted.
     */
    public boolean deplete(int x, int y) {
        if (!inBounds(x, y)) return false;
        int rem = resourceAt(x, y);
        if (rem <= 0) return false;
        resource[y * width + x] = rem - 1;
        return true;
    }

    /** Starting reserve for a mineable terrain; 0 for non-mineable. Deterministic. */
    private static int defaultYield(TerrainType t) {
        switch (t) {
            case MOUNTAIN_STONE: return 40;
            case MOUNTAIN_COAL:  return 34;
            case MOUNTAIN_IRON:  return 30;
            case MOUNTAIN_GOLD:  return 20;
            default:             return 0;
        }
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
