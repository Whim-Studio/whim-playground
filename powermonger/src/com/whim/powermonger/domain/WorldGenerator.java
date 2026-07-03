package com.whim.powermonger.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.whim.powermonger.api.Enums.Allegiance;
import com.whim.powermonger.api.Enums.Job;
import com.whim.powermonger.api.Enums.TerrainType;

/**
 * Deterministic procedural world generation. Given a seed it produces the same
 * terrain, towns, townspeople and captains every time (uses only
 * {@link java.util.Random}). Passive: it builds state, it does not simulate.
 */
public final class WorldGenerator {

    public static final int SIZE = 48;
    public static final int MAX_ELEVATION = 6;

    private static final String[] TOWN_NAMES = {
        "Aldermoor", "Blackfen", "Craghold", "Dunwich", "Evermarsh", "Fallow",
        "Greywater", "Highvale", "Ironford", "Keldale", "Longbarrow", "Mirefield",
        "Northwatch", "Oxbridge", "Pinehollow", "Rooksby", "Stonebridge", "Thornwick"
    };

    private WorldGenerator() {}

    public static WorldState generate(long seed) {
        Random rng = new Random(seed);
        MapGrid grid = new MapGrid(SIZE, SIZE, MAX_ELEVATION);

        double[][] height = buildHeightField(rng);
        assignTerrain(grid, height, rng);

        WorldState world = new WorldState(grid);

        placeTowns(world, rng);
        placeTownspeople(world, rng);
        placeCaptains(world, rng);

        world.setStatusMessage("A new realm awaits your ambition.");
        return world;
    }

    // --- terrain -----------------------------------------------------------

    /** Value-noise style height field via averaged random octaves, in [0,1]. */
    private static double[][] buildHeightField(Random rng) {
        double[][] h = new double[SIZE][SIZE];

        // Several coarse control grids blended for smooth elevation bands.
        int[] cellSizes = { 16, 8, 4 };
        double[] weights = { 0.6, 0.3, 0.1 };
        for (int o = 0; o < cellSizes.length; o++) {
            int cs = cellSizes[o];
            int gw = SIZE / cs + 2;
            double[][] ctrl = new double[gw][gw];
            for (int i = 0; i < gw; i++) {
                for (int j = 0; j < gw; j++) {
                    ctrl[i][j] = rng.nextDouble();
                }
            }
            for (int x = 0; x < SIZE; x++) {
                for (int y = 0; y < SIZE; y++) {
                    int gx = x / cs, gy = y / cs;
                    double fx = (x % cs) / (double) cs;
                    double fy = (y % cs) / (double) cs;
                    double v00 = ctrl[gx][gy], v10 = ctrl[gx + 1][gy];
                    double v01 = ctrl[gx][gy + 1], v11 = ctrl[gx + 1][gy + 1];
                    double top = lerp(v00, v10, smooth(fx));
                    double bot = lerp(v01, v11, smooth(fx));
                    h[x][y] += weights[o] * lerp(top, bot, smooth(fy));
                }
            }
        }

        // Radial falloff so the landmass sits amid water (island feel).
        double cx = SIZE / 2.0, cy = SIZE / 2.0, maxR = SIZE / 2.0;
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                double dx = (x - cx) / maxR, dy = (y - cy) / maxR;
                double d = Math.sqrt(dx * dx + dy * dy);
                h[x][y] = h[x][y] * (1.0 - 0.45 * Math.min(1.0, d));
            }
        }

        // Blended octaves of uniform noise cluster around the mean, so stretch
        // the field back to full [0,1] to recover real elevation bands
        // (forests, hills, mountains), then add contrast so peaks stand out.
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (h[x][y] < min) min = h[x][y];
                if (h[x][y] > max) max = h[x][y];
            }
        }
        double range = (max - min) < 1e-9 ? 1.0 : (max - min);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                double n = (h[x][y] - min) / range;      // 0..1
                h[x][y] = Math.pow(n, 0.85);             // mild contrast toward highs
            }
        }
        return h;
    }

    private static void assignTerrain(MapGrid grid, double[][] h, Random rng) {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                double v = h[x][y];
                TerrainType terrain;
                int elevation;
                if (v < 0.28) { terrain = TerrainType.DEEP_WATER; elevation = 0; }
                else if (v < 0.36) { terrain = TerrainType.SHALLOW_WATER; elevation = 0; }
                else if (v < 0.40) { terrain = TerrainType.BEACH; elevation = 0; }
                else if (v < 0.62) { terrain = TerrainType.GRASS; elevation = 1; }
                else if (v < 0.74) { terrain = TerrainType.FOREST; elevation = 2; }
                else if (v < 0.86) { terrain = TerrainType.HILL; elevation = 4; }
                else { terrain = TerrainType.MOUNTAIN; elevation = 6; }

                Tile t = new Tile(x, y, terrain, elevation);
                switch (terrain) {
                    case FOREST:
                        t.setTrees(true);
                        t.setFoodPotential(30 + rng.nextInt(30));
                        break;
                    case GRASS:
                        t.setFoodPotential(40 + rng.nextInt(40));
                        if (rng.nextDouble() < 0.12) t.setTrees(true);
                        break;
                    case SHALLOW_WATER:
                        t.setFoodPotential(35 + rng.nextInt(35)); // fishing
                        break;
                    case BEACH:
                        t.setFoodPotential(10 + rng.nextInt(15));
                        break;
                    case HILL:
                        t.setFoodPotential(10 + rng.nextInt(20));
                        break;
                    default:
                        t.setFoodPotential(0);
                        break;
                }
                grid.setTile(x, y, t);
            }
        }
    }

    private static boolean isLandBuildable(Tile t) {
        if (t == null) return false;
        TerrainType tt = t.terrain();
        return tt == TerrainType.GRASS || tt == TerrainType.FOREST
                || tt == TerrainType.BEACH || tt == TerrainType.HILL;
    }

    // --- towns -------------------------------------------------------------

    private static void placeTowns(WorldState world, Random rng) {
        MapGrid grid = world.grid();
        int wanted = 6 + rng.nextInt(5); // 6..10
        int minGap = 5;
        List<int[]> placed = new ArrayList<int[]>();
        int attempts = 0;
        int id = 0;
        while (placed.size() < wanted && attempts < 4000) {
            attempts++;
            int x = rng.nextInt(SIZE), y = rng.nextInt(SIZE);
            Tile t = grid.tile(x, y);
            if (!isLandBuildable(t)) continue;
            boolean tooClose = false;
            for (int i = 0; i < placed.size(); i++) {
                int[] p = placed.get(i);
                if (Math.abs(p[0] - x) + Math.abs(p[1] - y) < minGap) { tooClose = true; break; }
            }
            if (tooClose) continue;

            t.setTerrain(TerrainType.TOWN);
            t.setTrees(false);
            t.setTownId(id);
            String name = TOWN_NAMES[id % TOWN_NAMES.length];
            int pop = 40 + rng.nextInt(160);
            world.towns().add(new Town(id, x, y, name, pop, Allegiance.NEUTRAL));
            placed.add(new int[] { x, y });
            id++;
        }
    }

    // --- townspeople -------------------------------------------------------

    private static void placeTownspeople(WorldState world, Random rng) {
        MapGrid grid = world.grid();
        List<Town> towns = world.towns();
        int count = 40 + rng.nextInt(6); // ~40
        for (int i = 0; i < count; i++) {
            // Spawn near a random town.
            Town home = towns.isEmpty() ? null : towns.get(rng.nextInt(towns.size()));
            double px, py;
            if (home != null) {
                px = home.tileX() + (rng.nextDouble() - 0.5) * 6.0;
                py = home.tileY() + (rng.nextDouble() - 0.5) * 6.0;
            } else {
                px = rng.nextInt(SIZE);
                py = rng.nextInt(SIZE);
            }
            px = clampCoord(px);
            py = clampCoord(py);
            Job job = jobForTile(grid.tile((int) px, (int) py), rng);
            world.townspeople().add(new Townsperson(i, px, py, job, Allegiance.NEUTRAL));
        }
    }

    private static Job jobForTile(Tile t, Random rng) {
        if (t == null) return Job.IDLE;
        switch (t.terrain()) {
            case GRASS: return rng.nextBoolean() ? Job.FARMING : Job.HERDING;
            case FOREST: return Job.CRAFTING;
            case SHALLOW_WATER:
            case BEACH: return Job.FISHING;
            case TOWN: return Job.CRAFTING;
            default: return Job.IDLE;
        }
    }

    // --- captains ----------------------------------------------------------

    private static final String[] PLAYER_NAMES = { "Cardinal", "Ausgord", "Baldwin", "Ceril" };
    private static final String[] ENEMY_NAMES = { "Vashet", "Morkul", "Draygan", "Skorn" };

    private static void placeCaptains(WorldState world, Random rng) {
        List<Town> towns = world.towns();
        int id = 0;

        // Player captains cluster near one town.
        Town base = towns.isEmpty() ? null : towns.get(rng.nextInt(towns.size()));
        double bx = base != null ? base.tileX() : SIZE / 2.0;
        double by = base != null ? base.tileY() : SIZE / 2.0;
        for (int i = 0; i < 4; i++) {
            double x = clampCoord(bx + (rng.nextDouble() - 0.5) * 4.0);
            double y = clampCoord(by + (rng.nextDouble() - 0.5) * 4.0);
            ArmyBloc bloc = new ArmyBloc(30 + rng.nextInt(40), 40 + rng.nextInt(40));
            boolean supreme = (i == 0);
            Captain c = new Captain(id, PLAYER_NAMES[i % PLAYER_NAMES.length],
                    x, y, Allegiance.PLAYER, bloc, supreme);
            if (supreme) c.setSelected(true);
            world.captains().add(c);
            id++;
        }

        // 2..3 enemy captains placed far from the player base.
        int enemies = 2 + rng.nextInt(2);
        int placed = 0, attempts = 0;
        while (placed < enemies && attempts < 2000) {
            attempts++;
            Town t = towns.isEmpty() ? null : towns.get(rng.nextInt(towns.size()));
            double x = t != null ? t.tileX() + (rng.nextDouble() - 0.5) * 4.0
                                 : rng.nextInt(SIZE);
            double y = t != null ? t.tileY() + (rng.nextDouble() - 0.5) * 4.0
                                 : rng.nextInt(SIZE);
            if (Math.abs(x - bx) + Math.abs(y - by) < 18) continue; // keep enemies distant
            x = clampCoord(x);
            y = clampCoord(y);
            ArmyBloc bloc = new ArmyBloc(30 + rng.nextInt(50), 30 + rng.nextInt(40));
            world.captains().add(new Captain(id, ENEMY_NAMES[placed % ENEMY_NAMES.length],
                    x, y, Allegiance.ENEMY, bloc, false));
            id++;
            placed++;
        }
        // Fallback: if distance constraint starved us, place remaining anywhere.
        while (placed < enemies) {
            double x = clampCoord(rng.nextInt(SIZE));
            double y = clampCoord(rng.nextInt(SIZE));
            ArmyBloc bloc = new ArmyBloc(30 + rng.nextInt(50), 30 + rng.nextInt(40));
            world.captains().add(new Captain(id, ENEMY_NAMES[placed % ENEMY_NAMES.length],
                    x, y, Allegiance.ENEMY, bloc, false));
            id++;
            placed++;
        }
    }

    // --- helpers -----------------------------------------------------------

    private static double clampCoord(double v) {
        if (v < 0) return 0;
        if (v > SIZE - 1) return SIZE - 1;
        return v;
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    private static double smooth(double t) { return t * t * (3 - 2 * t); }
}
