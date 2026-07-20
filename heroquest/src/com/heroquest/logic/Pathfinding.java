package com.heroquest.logic;

import com.heroquest.model.DungeonMap;
import com.heroquest.model.GameState;
import com.heroquest.model.Point;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Orthogonal breadth-first search over walkable, unoccupied squares. Used for
 * Hero movement (path length must fit the movement allowance) and for Zargon's
 * monsters to close on the Heroes.
 */
public final class Pathfinding {

    private static final int[][] STEPS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private Pathfinding() {
    }

    /**
     * Shortest orthogonal path from start to goal across empty walkable squares.
     * Returns the ordered list of squares after {@code start} (goal last), or an
     * empty list if unreachable. {@code allowGoalOccupied} lets the goal square be
     * occupied (e.g. stepping to attack is handled separately) — normally false.
     */
    public static List<Point> shortestPath(GameState state, Point start, Point goal) {
        DungeonMap map = state.getMap();
        if (start.equals(goal)) {
            return new ArrayList<Point>();
        }
        Queue<Point> frontier = new ArrayDeque<Point>();
        Map<Point, Point> cameFrom = new HashMap<Point, Point>();
        frontier.add(start);
        cameFrom.put(start, start);

        while (!frontier.isEmpty()) {
            Point cur = frontier.poll();
            if (cur.equals(goal)) {
                return reconstruct(cameFrom, start, goal);
            }
            for (int[] s : STEPS) {
                Point next = cur.translate(s[0], s[1]);
                if (cameFrom.containsKey(next)) {
                    continue;
                }
                if (!map.isWalkable(next)) {
                    continue;
                }
                // The goal may be occupied by the mover's target; every other
                // occupied square blocks movement.
                if (state.isOccupied(next) && !next.equals(goal)) {
                    continue;
                }
                cameFrom.put(next, cur);
                frontier.add(next);
            }
        }
        return new ArrayList<Point>();
    }

    private static List<Point> reconstruct(Map<Point, Point> cameFrom, Point start, Point goal) {
        List<Point> path = new ArrayList<Point>();
        Point cur = goal;
        while (!cur.equals(start)) {
            path.add(cur);
            cur = cameFrom.get(cur);
        }
        java.util.Collections.reverse(path);
        return path;
    }
}
