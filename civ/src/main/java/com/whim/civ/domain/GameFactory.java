package com.whim.civ.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Factory producing a ready-to-play GameState (map, civs, starting settlers/militia). */
public final class GameFactory {

    private GameFactory() { }

    private static final String[] CIV_NAMES = {
            "Romans", "Babylonians", "Germans", "Egyptians", "Americans",
            "Greeks", "Indians", "Russians", "Zulus", "French",
            "Aztecs", "Chinese", "English", "Mongols"
    };

    // Land terrain palette, weighted by repetition (deterministic distribution).
    private static final Terrain[] LAND_PALETTE = {
            Terrain.GRASSLAND, Terrain.GRASSLAND, Terrain.GRASSLAND,
            Terrain.PLAINS, Terrain.PLAINS,
            Terrain.FOREST, Terrain.FOREST,
            Terrain.HILLS,
            Terrain.MOUNTAINS,
            Terrain.DESERT,
            Terrain.TUNDRA,
            Terrain.SWAMP,
            Terrain.JUNGLE
    };

    /**
     * Builds a deterministic, ready-to-play game: a seeded map, one human civ plus rivals
     * (each in Despotism with default rates), and for every civ two Settlers + one Militia
     * placed near a land start tile.
     */
    public static GameState newStandardGame(int width, int height, int numCivs, long seed) {
        if (numCivs < 1) {
            numCivs = 1;
        }
        Random rng = new Random(seed);
        GameMap map = generateMap(width, height, rng);

        GameState state = new GameState(map);

        List<int[]> starts = chooseStarts(map, numCivs, rng);

        for (int i = 0; i < numCivs; i++) {
            boolean human = (i == 0);
            String name = CIV_NAMES[i % CIV_NAMES.length];
            Civilization civ = new Civilization(i, name, human);
            civ.setGovernment(Government.DESPOTISM);
            civ.setRates(40, 60, 0);                 // default Civ1-style rates
            civ.setResearching(TechType.POTTERY);    // a root tech (always researchable)
            state.getCivilizations().add(civ);

            int[] start = starts.get(i);
            placeStartingUnits(state, map, civ.getId(), start[0], start[1]);
        }

        state.setActiveCivIndex(0);
        return state;
    }

    private static GameMap generateMap(int width, int height, Random rng) {
        GameMap map = new GameMap(width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Tile tile = map.getTile(x, y);
                boolean edge = (x == 0 || y == 0 || x == width - 1 || y == height - 1);
                if (edge) {
                    tile.setTerrain(Terrain.OCEAN);
                } else if (rng.nextInt(100) < 60) {
                    tile.setTerrain(LAND_PALETTE[rng.nextInt(LAND_PALETTE.length)]);
                } else {
                    tile.setTerrain(Terrain.OCEAN);
                }
            }
        }
        return map;
    }

    /** Collects land tiles and greedily spreads civ starts apart for fairness. */
    private static List<int[]> chooseStarts(GameMap map, int numCivs, Random rng) {
        List<int[]> land = new ArrayList<int[]>();
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                if (isLand(map.getTile(x, y).getTerrain())) {
                    land.add(new int[]{x, y});
                }
            }
        }

        List<int[]> starts = new ArrayList<int[]>();
        if (land.isEmpty()) {
            // Degenerate map with no land: carve a grassland tile so the game is playable.
            int cx = map.getWidth() / 2;
            int cy = map.getHeight() / 2;
            map.getTile(cx, cy).setTerrain(Terrain.GRASSLAND);
            for (int i = 0; i < numCivs; i++) {
                starts.add(new int[]{cx, cy});
            }
            return starts;
        }

        // Desired minimum spacing scales with map size and civ count.
        int span = Math.max(map.getWidth(), map.getHeight());
        int minDist = Math.max(2, span / (numCivs + 1));

        int attempts = 0;
        while (starts.size() < numCivs && attempts < 5000) {
            attempts++;
            int[] candidate = land.get(rng.nextInt(land.size()));
            boolean ok = true;
            for (int[] s : starts) {
                if (chebyshev(candidate, s) < minDist) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                starts.add(candidate);
            }
            // Relax spacing if we are struggling to place everyone.
            if (attempts % 500 == 0 && minDist > 2) {
                minDist--;
            }
        }
        // Fallback: fill any remaining civs with arbitrary land tiles.
        int idx = 0;
        while (starts.size() < numCivs) {
            starts.add(land.get(idx % land.size()));
            idx++;
        }
        return starts;
    }

    private static void placeStartingUnits(GameState state, GameMap map, int civId,
                                           int sx, int sy) {
        // Two Settlers + one Militia, spread over the start tile and adjacent land tiles.
        List<int[]> spots = new ArrayList<int[]>();
        spots.add(new int[]{sx, sy});
        for (int[] n : map.neighbors(sx, sy)) {
            if (isLand(map.getTile(n[0], n[1]).getTerrain())) {
                spots.add(n);
            }
        }

        UnitType[] roster = {UnitType.SETTLERS, UnitType.SETTLERS, UnitType.MILITIA};
        for (int i = 0; i < roster.length; i++) {
            int[] spot = spots.get(i % spots.size());
            state.getUnits().add(new Unit(roster[i], civId, spot[0], spot[1]));
        }
    }

    private static int chebyshev(int[] a, int[] b) {
        return Math.max(Math.abs(a[0] - b[0]), Math.abs(a[1] - b[1]));
    }

    private static boolean isLand(Terrain t) {
        return t != Terrain.OCEAN;
    }
}
