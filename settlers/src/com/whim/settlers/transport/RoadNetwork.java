package com.whim.settlers.transport;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The flag/road graph. Owns flags and roads and answers the routing questions the
 * transport system needs: which road to take next toward a destination
 * ({@link #nextHop}) and whether two flags are connected ({@link #connected}).
 * Routing is breadth-first over the flag graph (few nodes), giving fewest-hops
 * paths — good enough and cheap.
 */
public final class RoadNetwork {

    private final Map<Integer, Flag> flags = new HashMap<Integer, Flag>();
    private final List<Road> roads = new ArrayList<Road>();
    private final Map<Integer, List<Road>> adjacency = new HashMap<Integer, List<Road>>();
    private int nextFlagId = 1;
    private int nextRoadId = 1;

    public Flag addFlag(int x, int y) {
        Flag existing = flagAt(x, y);
        if (existing != null) return existing;
        Flag f = new Flag(nextFlagId++, x, y);
        flags.put(f.id(), f);
        adjacency.put(f.id(), new ArrayList<Road>());
        return f;
    }

    public Flag flagAt(int x, int y) {
        for (Flag f : flags.values()) {
            if (f.x() == x && f.y() == y) return f;
        }
        return null;
    }

    public Flag flag(int id) { return flags.get(id); }

    public java.util.Collection<Flag> flags() { return flags.values(); }
    public List<Road> roads() { return roads; }

    /** Add a road along {@code path} between two existing flags. */
    public Road addRoad(int flagA, int flagB, List<int[]> path) {
        if (flagA == flagB || !flags.containsKey(flagA) || !flags.containsKey(flagB)) return null;
        Road r = new Road(nextRoadId++, flagA, flagB, path);
        roads.add(r);
        adjacency.get(flagA).add(r);
        adjacency.get(flagB).add(r);
        return r;
    }

    public List<Road> roadsAt(int flagId) {
        List<Road> r = adjacency.get(flagId);
        return r == null ? java.util.Collections.<Road>emptyList() : r;
    }

    /** The first road to travel from {@code from} toward {@code dest}, or null. */
    public Road nextHop(int from, int dest) {
        if (from == dest) return null;
        Map<Integer, Road> firstRoad = new HashMap<Integer, Road>();
        Map<Integer, Integer> prev = new HashMap<Integer, Integer>();
        ArrayDeque<Integer> q = new ArrayDeque<Integer>();
        q.add(from);
        prev.put(from, from);
        while (!q.isEmpty()) {
            int cur = q.poll();
            for (Road road : roadsAt(cur)) {
                int nb = road.otherEnd(cur);
                if (prev.containsKey(nb)) continue;
                prev.put(nb, cur);
                firstRoad.put(nb, cur == from ? road : firstRoad.get(cur));
                if (nb == dest) return firstRoad.get(nb);
                q.add(nb);
            }
        }
        return null;
    }

    public boolean connected(int a, int b) {
        if (a == b) return true;
        ArrayDeque<Integer> q = new ArrayDeque<Integer>();
        Map<Integer, Boolean> seen = new HashMap<Integer, Boolean>();
        q.add(a); seen.put(a, true);
        while (!q.isEmpty()) {
            int cur = q.poll();
            for (Road road : roadsAt(cur)) {
                int nb = road.otherEnd(cur);
                if (seen.containsKey(nb)) continue;
                if (nb == b) return true;
                seen.put(nb, true);
                q.add(nb);
            }
        }
        return false;
    }

    /** Whether a direct road already joins these two flags. */
    public boolean directRoadExists(int a, int b) {
        for (Road r : roadsAt(a)) {
            if (r.otherEnd(a) == b) return true;
        }
        return false;
    }
}
