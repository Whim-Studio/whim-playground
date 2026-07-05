package com.whim.kenshi.domain;

import com.whim.kenshi.api.Config;
import com.whim.kenshi.api.Enums.Terrain;

import java.util.Random;

/**
 * A {@code MAP_TILES × MAP_TILES} grid of {@link Terrain}, generated
 * deterministically from a seed. Uses layered value-noise (elevation +
 * moisture) to lay down deserts, scrub, green belts, rock highlands, ash
 * flats, and lakes, then stamps square TOWN patches at requested tiles.
 *
 * <p>Terrain is immutable after construction except for {@link #stampTown},
 * which {@link WorldBuilder} calls before the world goes live.
 */
public final class MapGrid {

    private final int tiles = Config.MAP_TILES;
    private final Terrain[][] grid; // [col][row]

    public MapGrid(long seed) {
        grid = new Terrain[tiles][tiles];
        generate(seed);
    }

    public int tiles() { return tiles; }
    public double tileSize() { return Config.TILE_SIZE; }

    public Terrain terrain(int col, int row) {
        if (col < 0 || row < 0 || col >= tiles || row >= tiles) return Terrain.ROCK;
        return grid[col][row];
    }

    /** True if a tile blocks movement (water). Convenience for the pathfinder. */
    public boolean blocked(int col, int row) {
        return terrain(col, row) == Terrain.WATER;
    }

    /** Convert a world coordinate to a tile column (clamped). */
    public int colOf(double worldX) {
        int c = (int) Math.floor(worldX / Config.TILE_SIZE);
        if (c < 0) c = 0;
        if (c >= tiles) c = tiles - 1;
        return c;
    }

    /** Convert a world coordinate to a tile row (clamped). */
    public int rowOf(double worldY) {
        int r = (int) Math.floor(worldY / Config.TILE_SIZE);
        if (r < 0) r = 0;
        if (r >= tiles) r = tiles - 1;
        return r;
    }

    /** Stamp a square TOWN patch of the given half-extent (in tiles). */
    public void stampTown(int centerCol, int centerRow, int halfTiles) {
        for (int c = centerCol - halfTiles; c <= centerCol + halfTiles; c++) {
            for (int r = centerRow - halfTiles; r <= centerRow + halfTiles; r++) {
                if (c < 0 || r < 0 || c >= tiles || r >= tiles) continue;
                grid[c][r] = Terrain.TOWN;
            }
        }
    }

    // ---- generation -----------------------------------------------------

    private void generate(long seed) {
        double[][] elevation = fractalNoise(seed * 31 + 1, 4);
        double[][] moisture = fractalNoise(seed * 17 + 7, 4);

        for (int c = 0; c < tiles; c++) {
            for (int r = 0; r < tiles; r++) {
                grid[c][r] = classify(elevation[c][r], moisture[c][r]);
            }
        }
    }

    /** Map (elevation, moisture) in [0,1] to a terrain type. */
    private Terrain classify(double e, double m) {
        if (e < 0.30) return Terrain.WATER;          // low basins → lakes
        if (e > 0.80) return Terrain.ROCK;           // highlands → rock
        if (e > 0.68) return m < 0.35 ? Terrain.ASH : Terrain.ROCK; // ridges
        // mid elevations vary by moisture
        if (m > 0.66) return Terrain.GREEN;          // fertile belts
        if (m > 0.42) return Terrain.SCRUB;          // patchy scrub
        return Terrain.SAND;                          // desert
    }

    /**
     * Sum a few octaves of value noise into a normalised [0,1] field.
     */
    private double[][] fractalNoise(long seed, int octaves) {
        double[][] out = new double[tiles][tiles];
        double amp = 1.0;
        double totalAmp = 0.0;
        int cells = 4; // lattice resolution of the first octave

        for (int o = 0; o < octaves; o++) {
            double[][] layer = valueNoise(seed + o * 1013L, cells);
            for (int c = 0; c < tiles; c++) {
                for (int r = 0; r < tiles; r++) {
                    out[c][r] += layer[c][r] * amp;
                }
            }
            totalAmp += amp;
            amp *= 0.5;
            cells *= 2;
        }
        // normalise
        for (int c = 0; c < tiles; c++) {
            for (int r = 0; r < tiles; r++) {
                out[c][r] /= totalAmp;
            }
        }
        return out;
    }

    /**
     * One octave of value noise: random values on a coarse lattice, bilinearly
     * interpolated (smoothstep) up to the full tile grid.
     */
    private double[][] valueNoise(long seed, int cells) {
        Random rng = new Random(seed);
        int gp = cells + 1;
        double[][] lattice = new double[gp][gp];
        for (int i = 0; i < gp; i++) {
            for (int j = 0; j < gp; j++) {
                lattice[i][j] = rng.nextDouble();
            }
        }

        double[][] out = new double[tiles][tiles];
        double scale = (double) cells / tiles;
        for (int c = 0; c < tiles; c++) {
            for (int r = 0; r < tiles; r++) {
                double fx = c * scale;
                double fy = r * scale;
                int x0 = (int) Math.floor(fx);
                int y0 = (int) Math.floor(fy);
                int x1 = Math.min(x0 + 1, cells);
                int y1 = Math.min(y0 + 1, cells);
                double tx = smooth(fx - x0);
                double ty = smooth(fy - y0);
                double top = lerp(lattice[x0][y0], lattice[x1][y0], tx);
                double bot = lerp(lattice[x0][y1], lattice[x1][y1], tx);
                out[c][r] = lerp(top, bot, ty);
            }
        }
        return out;
    }

    private static double smooth(double t) { return t * t * (3 - 2 * t); }
    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
}
