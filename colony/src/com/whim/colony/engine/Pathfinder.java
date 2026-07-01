package com.whim.colony.engine;

import com.whim.colony.domain.ColonyMap;
import com.whim.colony.domain.MapTile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * A stateless A* pathfinder over a {@link ColonyMap}. Movement is 4-neighbour
 * (orthogonal) and only enters walkable tiles (see {@link MapTile#isWalkable()}).
 * The heuristic is Manhattan distance, which is admissible for unit-cost
 * orthogonal movement, so the returned path is optimal in step count.
 *
 * <p>Instances hold no state and are safe to reuse/share; each {@code findPath}
 * call is self-contained.
 */
public final class Pathfinder {

    /** The four orthogonal neighbour offsets (N, S, W, E). */
    private static final int[][] NEIGHBOURS = {
            {0, -1}, {0, 1}, {-1, 0}, {1, 0}
    };

    /**
     * Find the shortest walkable path from (sx,sy) to (tx,ty).
     *
     * <p>The returned list contains the ordered step tiles the colonist should
     * walk through, EXCLUDING the start tile and INCLUDING the target tile. An
     * empty list means either the start already equals the target, or the target
     * is unreachable/blocked/out of bounds.
     *
     * @return a fresh mutable list of {int[]{x,y}} steps; never {@code null}.
     */
    public List<int[]> findPath(ColonyMap map, int sx, int sy, int tx, int ty) {
        List<int[]> empty = new ArrayList<int[]>();
        if (map == null) {
            return empty;
        }
        if (!map.inBounds(sx, sy) || !map.inBounds(tx, ty)) {
            return empty;
        }
        if (sx == tx && sy == ty) {
            return empty;
        }
        MapTile target = map.getTile(tx, ty);
        if (target == null || !target.isWalkable()) {
            return empty;
        }

        final int width = map.getWidth();
        // Encode (x,y) as a single int key for the maps: x * height + y.
        final int height = map.getHeight();

        Map<Integer, Integer> gScore = new HashMap<Integer, Integer>();
        Map<Integer, Integer> cameFrom = new HashMap<Integer, Integer>();

        final int startKey = key(sx, sy, height);
        final int targetKey = key(tx, ty, height);
        gScore.put(startKey, 0);

        PriorityQueue<Node> open = new PriorityQueue<Node>(new Comparator<Node>() {
            public int compare(Node a, Node b) {
                return a.f - b.f;
            }
        });
        open.add(new Node(sx, sy, 0, manhattan(sx, sy, tx, ty)));

        while (!open.isEmpty()) {
            Node current = open.poll();
            int curKey = key(current.x, current.y, height);

            // A stale queue entry (a better path to this node was already found).
            Integer bestG = gScore.get(curKey);
            if (bestG != null && current.g > bestG.intValue()) {
                continue;
            }

            if (curKey == targetKey) {
                return reconstruct(cameFrom, targetKey, startKey, height);
            }

            for (int[] d : NEIGHBOURS) {
                int nx = current.x + d[0];
                int ny = current.y + d[1];
                if (!map.inBounds(nx, ny)) {
                    continue;
                }
                MapTile tile = map.getTile(nx, ny);
                if (tile == null || !tile.isWalkable()) {
                    continue;
                }
                int tentativeG = current.g + 1;
                int nKey = key(nx, ny, height);
                Integer known = gScore.get(nKey);
                if (known == null || tentativeG < known.intValue()) {
                    gScore.put(nKey, tentativeG);
                    cameFrom.put(nKey, curKey);
                    int f = tentativeG + manhattan(nx, ny, tx, ty);
                    open.add(new Node(nx, ny, tentativeG, f));
                }
            }
        }
        // Unreachable.
        return empty;
    }

    /** Walk the cameFrom chain back from target to start and reverse it. */
    private List<int[]> reconstruct(Map<Integer, Integer> cameFrom, int targetKey,
                                    int startKey, int height) {
        List<int[]> path = new ArrayList<int[]>();
        int cur = targetKey;
        while (cur != startKey) {
            path.add(new int[]{cur / height, cur % height});
            Integer prev = cameFrom.get(cur);
            if (prev == null) {
                // Should not happen once target was reached, but stay defensive.
                return new ArrayList<int[]>();
            }
            cur = prev.intValue();
        }
        // path currently runs target -> ... -> first-step; reverse to walking order.
        java.util.Collections.reverse(path);
        return path;
    }

    private static int key(int x, int y, int height) {
        return x * height + y;
    }

    private static int manhattan(int x0, int y0, int x1, int y1) {
        return Math.abs(x0 - x1) + Math.abs(y0 - y1);
    }

    /** A single frontier entry: coordinates, cost-so-far g, and priority f=g+h. */
    private static final class Node {
        final int x;
        final int y;
        final int g;
        final int f;

        Node(int x, int y, int g, int f) {
            this.x = x;
            this.y = y;
            this.g = g;
            this.f = f;
        }
    }
}
