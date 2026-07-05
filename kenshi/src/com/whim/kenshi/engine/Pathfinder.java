package com.whim.kenshi.engine;

import com.whim.kenshi.api.Config;
import com.whim.kenshi.api.Enums.Terrain;
import com.whim.kenshi.domain.MapGrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Grid A* over the {@link MapGrid}. Tiles whose terrain is {@link Terrain#WATER}
 * are impassable; movement is 8-connected with octile costs. Results are returned
 * as a list of world-unit waypoints (tile centres) ending at the exact goal
 * position. If the goal tile is water, the nearest passable tile is used instead.
 *
 * <p>Stateless and thread-confined to the tick thread. A hard expansion cap keeps
 * a pathological query bounded on the 96×96 grid.</p>
 */
final class Pathfinder {

    private static final int MAX_EXPANSIONS = 20000;
    private static final int TILES = Config.MAP_TILES;

    private final MapGrid map;

    Pathfinder(MapGrid map) {
        this.map = map;
    }

    private boolean passable(int col, int row) {
        if (col < 0 || row < 0 || col >= TILES || row >= TILES) {
            return false;
        }
        return map.terrain(col, row) != Terrain.WATER;
    }

    private static int col(double worldX) {
        int c = (int) Math.floor(worldX / Config.TILE_SIZE);
        if (c < 0) { c = 0; }
        if (c >= TILES) { c = TILES - 1; }
        return c;
    }

    private static int row(double worldY) {
        int r = (int) Math.floor(worldY / Config.TILE_SIZE);
        if (r < 0) { r = 0; }
        if (r >= TILES) { r = TILES - 1; }
        return r;
    }

    private static double centreX(int col) { return (col + 0.5) * Config.TILE_SIZE; }
    private static double centreY(int row) { return (row + 0.5) * Config.TILE_SIZE; }

    /** BFS-nearest passable tile around a (possibly water) target tile. */
    private int[] nearestPassable(int col, int row) {
        if (passable(col, row)) {
            return new int[] { col, row };
        }
        for (int radius = 1; radius < TILES; radius++) {
            for (int dc = -radius; dc <= radius; dc++) {
                for (int dr = -radius; dr <= radius; dr++) {
                    if (Math.max(Math.abs(dc), Math.abs(dr)) != radius) {
                        continue;
                    }
                    int nc = col + dc;
                    int nr = row + dr;
                    if (passable(nc, nr)) {
                        return new int[] { nc, nr };
                    }
                }
            }
        }
        return new int[] { col, row };
    }

    /**
     * Compute a waypoint path (world units) from a world start to a world goal.
     * The returned list never includes the start tile centre and always ends at
     * the exact goal coordinates. Returns a single-element list (the goal) when
     * start and goal share a tile or no path is found.
     */
    List<double[]> path(double startX, double startY, double goalX, double goalY) {
        List<double[]> fallback = new ArrayList<double[]>(1);
        fallback.add(new double[] { goalX, goalY });

        int sc = col(startX);
        int sr = row(startY);
        int[] goalTile = nearestPassable(col(goalX), row(goalY));
        int gc = goalTile[0];
        int gr = goalTile[1];

        if (sc == gc && sr == gr) {
            return fallback;
        }
        if (!passable(sc, sr)) {
            int[] near = nearestPassable(sc, sr);
            sc = near[0];
            sr = near[1];
        }

        final int startIdx = sc + sr * TILES;
        final int goalIdx = gc + gr * TILES;

        Map<Integer, Integer> cameFrom = new HashMap<Integer, Integer>();
        Map<Integer, Double> gScore = new HashMap<Integer, Double>();
        final Map<Integer, Double> fScore = new HashMap<Integer, Double>();
        PriorityQueue<Integer> open = new PriorityQueue<Integer>(64, new Comparator<Integer>() {
            public int compare(Integer a, Integer b) {
                double fa = fScore.containsKey(a) ? fScore.get(a) : Double.MAX_VALUE;
                double fb = fScore.containsKey(b) ? fScore.get(b) : Double.MAX_VALUE;
                return Double.compare(fa, fb);
            }
        });

        gScore.put(startIdx, 0.0);
        fScore.put(startIdx, heuristic(sc, sr, gc, gr));
        open.add(startIdx);

        int expansions = 0;
        boolean found = false;
        while (!open.isEmpty() && expansions < MAX_EXPANSIONS) {
            int current = open.poll();
            expansions++;
            if (current == goalIdx) {
                found = true;
                break;
            }
            int cc = current % TILES;
            int cr = current / TILES;
            for (int dc = -1; dc <= 1; dc++) {
                for (int dr = -1; dr <= 1; dr++) {
                    if (dc == 0 && dr == 0) {
                        continue;
                    }
                    int nc = cc + dc;
                    int nr = cr + dr;
                    if (!passable(nc, nr)) {
                        continue;
                    }
                    // Disallow diagonal squeezes through two water corners.
                    if (dc != 0 && dr != 0) {
                        if (!passable(cc + dc, cr) && !passable(cc, cr + dr)) {
                            continue;
                        }
                    }
                    int neighbour = nc + nr * TILES;
                    double stepCost = (dc != 0 && dr != 0) ? 1.41421356 : 1.0;
                    double tentative = gScore.get(current) + stepCost;
                    double known = gScore.containsKey(neighbour) ? gScore.get(neighbour) : Double.MAX_VALUE;
                    if (tentative < known) {
                        cameFrom.put(neighbour, current);
                        gScore.put(neighbour, tentative);
                        fScore.put(neighbour, tentative + heuristic(nc, nr, gc, gr));
                        open.add(neighbour);
                    }
                }
            }
        }

        if (!found) {
            return fallback;
        }

        // Reconstruct, then convert to world waypoints.
        List<Integer> tiles = new ArrayList<Integer>();
        Integer cur = goalIdx;
        while (cur != null && cur != startIdx) {
            tiles.add(cur);
            cur = cameFrom.get(cur);
        }
        Collections.reverse(tiles);

        List<double[]> waypoints = new ArrayList<double[]>(tiles.size() + 1);
        for (int i = 0; i < tiles.size(); i++) {
            int idx = tiles.get(i);
            int wc = idx % TILES;
            int wr = idx / TILES;
            if (idx == goalIdx) {
                // Replace the final tile centre with the exact requested goal.
                waypoints.add(new double[] { goalX, goalY });
            } else {
                waypoints.add(new double[] { centreX(wc), centreY(wr) });
            }
        }
        if (waypoints.isEmpty()) {
            return fallback;
        }
        return waypoints;
    }

    private static double heuristic(int c0, int r0, int c1, int r1) {
        int dc = Math.abs(c0 - c1);
        int dr = Math.abs(r0 - r1);
        int min = Math.min(dc, dr);
        int max = Math.max(dc, dr);
        return (max - min) + 1.41421356 * min;
    }
}
