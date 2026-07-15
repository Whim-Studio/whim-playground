package com.whim.settlers.transport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * 4-directional A* over the tile grid, used to route a road between two flags.
 * Endpoints are always allowed (they sit on flag tiles); intermediate tiles must
 * satisfy the supplied {@link TilePredicate}. Returns the tile path inclusive of
 * both endpoints, or {@code null} if unreachable.
 */
public final class Pathfinder {

    private Pathfinder() { }

    private static final int[] DX = { 1, -1, 0, 0 };
    private static final int[] DY = { 0, 0, 1, -1 };

    public static List<int[]> find(int width, int height, TilePredicate walkable,
                                   int sx, int sy, int gx, int gy) {
        if (sx == gx && sy == gy) {
            List<int[]> p = new ArrayList<int[]>();
            p.add(new int[] { sx, sy });
            return p;
        }
        final int start = sy * width + sx;
        final int goal = gy * width + gx;
        int[] cameFrom = new int[width * height];
        int[] gScore = new int[width * height];
        java.util.Arrays.fill(cameFrom, -1);
        java.util.Arrays.fill(gScore, Integer.MAX_VALUE);
        gScore[start] = 0;

        PriorityQueue<int[]> open = new PriorityQueue<int[]>(new java.util.Comparator<int[]>() {
            @Override public int compare(int[] a, int[] b) { return a[1] - b[1]; }
        });
        open.add(new int[] { start, heuristic(sx, sy, gx, gy) });

        while (!open.isEmpty()) {
            int current = open.poll()[0];
            if (current == goal) return reconstruct(cameFrom, current, width);
            int cx = current % width, cy = current / width;
            for (int d = 0; d < 4; d++) {
                int nx = cx + DX[d], ny = cy + DY[d];
                if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue;
                boolean isGoal = (nx == gx && ny == gy);
                if (!isGoal && !walkable.test(nx, ny)) continue;
                int neighbor = ny * width + nx;
                int tentative = gScore[current] + 1;
                if (tentative < gScore[neighbor]) {
                    cameFrom[neighbor] = current;
                    gScore[neighbor] = tentative;
                    open.add(new int[] { neighbor, tentative + heuristic(nx, ny, gx, gy) });
                }
            }
        }
        return null;
    }

    private static int heuristic(int x, int y, int gx, int gy) {
        return Math.abs(x - gx) + Math.abs(y - gy);
    }

    private static List<int[]> reconstruct(int[] cameFrom, int current, int width) {
        // Walk parents from goal to start, prepending each so the result runs
        // start..goal.
        List<int[]> path = new ArrayList<int[]>();
        while (current != -1) {
            path.add(new int[] { current % width, current / width });
            current = cameFrom[current];
        }
        Collections.reverse(path);
        return path;
    }
}
