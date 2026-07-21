package com.railroad.logic;

import com.railroad.model.GridPoint;
import com.railroad.model.Industry;
import com.railroad.model.IndustryType;
import com.railroad.model.TerrainType;
import com.railroad.model.Tile;
import com.railroad.model.TileGrid;
import com.railroad.model.Town;
import com.railroad.model.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deterministic procedural map generator. Given a seed it always produces the
 * same world, so QA can reproduce a layout exactly.
 *
 * <p>Terrain is built from a small value-noise field: a coarse random grid of
 * heights is bilinearly interpolated up to full resolution, then thresholded
 * into water / clear / hills / mountains. Towns are then scattered onto clear
 * tiles, rejecting positions that are too close together so at least two sit a
 * meaningful distance apart for the first rail line.
 */
public final class MapGenerator {

    private static final String[] TOWN_NAMES = {
        "Ashford", "Belmont", "Cedar Falls", "Danforth", "Elmwood",
        "Fairhaven", "Granite City", "Holloway", "Ironton", "Junction Bay"
    };

    private final long seed;
    private final int width;
    private final int height;

    public MapGenerator(long seed, int width, int height) {
        this.seed = seed;
        this.width = width;
        this.height = height;
    }

    public World generate() {
        Random rng = new Random(seed);
        TileGrid grid = new TileGrid(width, height);
        double[][] heightField = buildHeightField(rng);
        applyTerrain(grid, heightField);
        List<Town> towns = placeTowns(grid, rng);
        List<Industry> industries = placeIndustries(grid, towns, rng);
        return new World(grid, towns, industries, seed);
    }

    /**
     * Places the one Phase 2 production chain on the map: a coal mine on rough
     * ground (hills/mountains, where coal belongs) and a steel mill next to a
     * town (so a single station serves both the town and the mill). If no ideal
     * tile is found the placement is skipped rather than forced onto water.
     */
    private List<Industry> placeIndustries(TileGrid grid, List<Town> towns, Random rng) {
        List<Industry> industries = new ArrayList<Industry>();

        GridPoint mine = findRoughTile(grid, rng);
        if (mine != null) {
            industries.add(new Industry(industries.size(), "Coal Mine",
                    IndustryType.COAL_MINE, mine));
        }

        // Put the mill on a clear tile adjacent to a town, avoiding the town tile
        // itself and any tile already taken by the mine.
        GridPoint mill = findClearAdjacentToTown(grid, towns, mine);
        if (mill != null) {
            industries.add(new Industry(industries.size(), "Steel Mill",
                    IndustryType.STEEL_MILL, mill));
        }
        return industries;
    }

    private GridPoint findRoughTile(TileGrid grid, Random rng) {
        for (int i = 0; i < 5000; i++) {
            int x = rng.nextInt(width);
            int y = rng.nextInt(height);
            TerrainType t = grid.terrainAt(x, y);
            if (t == TerrainType.HILLS || t == TerrainType.MOUNTAINS) {
                return new GridPoint(x, y);
            }
        }
        return null;
    }

    private GridPoint findClearAdjacentToTown(TileGrid grid, List<Town> towns, GridPoint taken) {
        for (Town t : towns) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }
                    int x = t.getX() + dx;
                    int y = t.getY() + dy;
                    if (!grid.inBounds(x, y) || grid.terrainAt(x, y) != TerrainType.CLEAR) {
                        continue;
                    }
                    GridPoint p = new GridPoint(x, y);
                    if (taken != null && taken.equals(p)) {
                        continue;
                    }
                    return p;
                }
            }
        }
        return null;
    }

    /** Coarse random control points bilinearly upsampled to the full grid. */
    private double[][] buildHeightField(Random rng) {
        int cellsX = 6;
        int cellsY = 5;
        double[][] control = new double[cellsX + 1][cellsY + 1];
        for (int cx = 0; cx <= cellsX; cx++) {
            for (int cy = 0; cy <= cellsY; cy++) {
                control[cx][cy] = rng.nextDouble();
            }
        }
        double[][] field = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double gx = (double) x / width * cellsX;
                double gy = (double) y / height * cellsY;
                int x0 = (int) gx;
                int y0 = (int) gy;
                double tx = gx - x0;
                double ty = gy - y0;
                double top = lerp(control[x0][y0], control[x0 + 1][y0], tx);
                double bot = lerp(control[x0][y0 + 1], control[x0 + 1][y0 + 1], tx);
                field[x][y] = lerp(top, bot, ty);
            }
        }
        return field;
    }

    private void applyTerrain(TileGrid grid, double[][] field) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double h = field[x][y];
                TerrainType terrain;
                if (h < 0.28) {
                    terrain = TerrainType.WATER;
                } else if (h < 0.68) {
                    terrain = TerrainType.CLEAR;
                } else if (h < 0.85) {
                    terrain = TerrainType.HILLS;
                } else {
                    terrain = TerrainType.MOUNTAINS;
                }
                grid.tileAt(x, y).setTerrain(terrain);
            }
        }
    }

    /**
     * Scatters towns on clear tiles. The first two are forced far apart (each in
     * opposite thirds of the map) so a starter line spans real distance; the rest
     * fill in wherever there is room, respecting a minimum separation.
     */
    private List<Town> placeTowns(TileGrid grid, Random rng) {
        List<Town> towns = new ArrayList<Town>();
        int minSeparation = Math.max(width, height) / 4;
        int targetCount = 4;

        // Force the first two into left- and right-third clear tiles.
        GridPoint left = findClearInBand(grid, rng, 0, width / 3);
        GridPoint right = findClearInBand(grid, rng, width * 2 / 3, width);
        if (left != null) {
            towns.add(new Town(towns.size(), TOWN_NAMES[towns.size()], left));
        }
        if (right != null && (left == null || dist(left, right) >= minSeparation)) {
            towns.add(new Town(towns.size(), TOWN_NAMES[towns.size()], right));
        }

        // Fill remaining towns anywhere clear, keeping them spread out.
        int attempts = 0;
        while (towns.size() < targetCount && attempts < 4000) {
            attempts++;
            int x = rng.nextInt(width);
            int y = rng.nextInt(height);
            if (grid.terrainAt(x, y) != TerrainType.CLEAR) {
                continue;
            }
            GridPoint p = new GridPoint(x, y);
            if (farFromAll(p, towns, minSeparation)) {
                towns.add(new Town(towns.size(), TOWN_NAMES[towns.size() % TOWN_NAMES.length], p));
            }
        }
        return towns;
    }

    private GridPoint findClearInBand(TileGrid grid, Random rng, int xMin, int xMax) {
        for (int i = 0; i < 3000; i++) {
            int x = xMin + rng.nextInt(Math.max(1, xMax - xMin));
            int y = rng.nextInt(height);
            if (grid.inBounds(x, y) && grid.terrainAt(x, y) == TerrainType.CLEAR) {
                return new GridPoint(x, y);
            }
        }
        return null;
    }

    private boolean farFromAll(GridPoint p, List<Town> towns, int minSeparation) {
        for (Town t : towns) {
            if (dist(p, t.getPosition()) < minSeparation) {
                return false;
            }
        }
        return true;
    }

    private static int dist(GridPoint a, GridPoint b) {
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
