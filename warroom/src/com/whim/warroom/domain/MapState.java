package com.whim.warroom.domain;

import java.util.Random;

/**
 * The terrain grid: a {@code cols x rows} array of {@link TerrainTile}s plus a
 * deterministic procedural generator.
 *
 * <p>World space is measured in {@code double} units; each tile is
 * {@link #TILE_SIZE} units square, so the world spans {@code cols*TILE_SIZE} by
 * {@code rows*TILE_SIZE}.
 */
public class MapState {
    public static final double TILE_SIZE = 32.0;

    private final int cols;
    private final int rows;
    private final TerrainTile[][] tiles;

    /** Fills the whole grid with GRASSLAND at a flat 0.5 elevation. */
    public MapState(int cols, int rows) {
        this.cols = Math.max(1, cols);
        this.rows = Math.max(1, rows);
        this.tiles = new TerrainTile[this.cols][this.rows];
        for (int c = 0; c < this.cols; c++) {
            for (int r = 0; r < this.rows; r++) {
                tiles[c][r] = new TerrainTile(c, r, Biome.GRASSLAND, 0.5);
            }
        }
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    public boolean inBounds(int c, int r) {
        return c >= 0 && r >= 0 && c < cols && r < rows;
    }

    /** Tile at (col,row), clamped to the grid so callers never index out of range. */
    public TerrainTile tile(int col, int row) {
        int c = clamp(col, 0, cols - 1);
        int r = clamp(row, 0, rows - 1);
        return tiles[c][r];
    }

    public double worldWidth() {
        return cols * TILE_SIZE;
    }

    public double worldHeight() {
        return rows * TILE_SIZE;
    }

    public TerrainTile tileAtWorld(double wx, double wy) {
        int c = (int) Math.floor(wx / TILE_SIZE);
        int r = (int) Math.floor(wy / TILE_SIZE);
        return tile(c, r);
    }

    public double moveCostMulAtWorld(double wx, double wy) {
        return tileAtWorld(wx, wy).getBiome().moveCostMul();
    }

    public double coverBonusAtWorld(double wx, double wy) {
        return tileAtWorld(wx, wy).getBiome().coverBonus();
    }

    // ---------------------------------------------------------------------
    // Procedural generation
    // ---------------------------------------------------------------------

    /**
     * Builds a deterministic map from a seed. Fractal value-noise produces an
     * elevation field (multiple octaves of a bilinearly-interpolated random
     * lattice); biomes are then assigned by elevation band, with {@code dominant}
     * filling the broad mid band so it forms the majority. Low elevation becomes
     * WATER, high becomes HILLS then SNOW. Same seed → byte-identical map.
     */
    public static MapState generate(int cols, int rows, long seed, Biome dominant) {
        MapState map = new MapState(cols, rows);
        Biome dom = (dominant == null) ? Biome.GRASSLAND : dominant;

        double[][] elev = fractalNoise(cols, rows, seed, 5, 0.5);
        // A second, independent field drives biome variety patches.
        double[][] variety = fractalNoise(cols, rows, seed ^ 0x9E3779B97F4A7C15L, 4, 0.55);

        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                double e = elev[c][r];
                double v = variety[c][r];
                TerrainTile t = map.tiles[c][r];
                t.setElevation(e);
                t.setBiome(assignBiome(e, v, dom));
            }
        }
        return map;
    }

    /** Elevation-band + variety-noise → biome. {@code dom} wins the wide mid band. */
    private static Biome assignBiome(double e, double v, Biome dom) {
        if (e < 0.24) {
            return Biome.WATER;
        }
        if (e >= 0.88) {
            return Biome.SNOW;
        }
        if (e >= 0.76) {
            return Biome.HILLS;
        }
        // Mid band: mostly the dominant biome, sprinkled with variety patches so
        // the field never looks flat. Patches stay a clear minority so the
        // dominant biome remains the overall majority for any seed.
        if (v > 0.82) {
            if (dom == Biome.FOREST) {
                return Biome.GRASSLAND;
            }
            if (dom == Biome.DESERT) {
                return Biome.HILLS;
            }
            if (dom == Biome.SNOW) {
                return Biome.HILLS;
            }
            return Biome.FOREST;
        }
        return dom;
    }

    /**
     * Fractal (fBm) value noise normalized to [0,1]. Sums {@code octaves} layers
     * of a random lattice, each octave doubling frequency and scaling amplitude
     * by {@code persistence}. Deterministic for a given seed.
     */
    private static double[][] fractalNoise(int cols, int rows, long seed, int octaves, double persistence) {
        double[][] out = new double[cols][rows];
        int maxDim = Math.max(cols, rows);

        double amplitude = 1.0;
        // Start with a coarse cell so the largest features span most of the map.
        int cell = Math.max(4, Integer.highestOneBit(Math.max(1, maxDim)));

        for (int o = 0; o < octaves; o++) {
            int cs = Math.max(1, cell);
            int latW = cols / cs + 2;
            int latH = rows / cs + 2;
            double[][] lattice = new double[latW][latH];
            Random rnd = new Random(seed * 0x100000001B3L + o * 0x2545F4914F6CDD1DL);
            for (int i = 0; i < latW; i++) {
                for (int j = 0; j < latH; j++) {
                    lattice[i][j] = rnd.nextDouble();
                }
            }
            for (int c = 0; c < cols; c++) {
                for (int r = 0; r < rows; r++) {
                    double gx = (double) c / cs;
                    double gy = (double) r / cs;
                    int ix = (int) Math.floor(gx);
                    int iy = (int) Math.floor(gy);
                    double fx = smooth(gx - ix);
                    double fy = smooth(gy - iy);
                    double v00 = lattice[ix][iy];
                    double v10 = lattice[ix + 1][iy];
                    double v01 = lattice[ix][iy + 1];
                    double v11 = lattice[ix + 1][iy + 1];
                    double top = v00 + (v10 - v00) * fx;
                    double bot = v01 + (v11 - v01) * fx;
                    out[c][r] += (top + (bot - top) * fy) * amplitude;
                }
            }
            amplitude *= persistence;
            cell = Math.max(1, cell / 2);
        }

        // Normalize to [0,1] deterministically from the observed range.
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                double val = out[c][r];
                if (val < min) {
                    min = val;
                }
                if (val > max) {
                    max = val;
                }
            }
        }
        double span = (max - min);
        if (span <= 0.0) {
            span = 1.0;
        }
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                out[c][r] = (out[c][r] - min) / span;
            }
        }
        return out;
    }

    /** Smoothstep easing for value-noise interpolation. */
    private static double smooth(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static int clamp(int v, int lo, int hi) {
        if (v < lo) {
            return lo;
        }
        if (v > hi) {
            return hi;
        }
        return v;
    }
}
