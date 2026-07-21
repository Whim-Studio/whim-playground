package com.railroad.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The player's rail network, modelled as an undirected graph whose nodes are
 * tiles ({@link GridPoint}) and whose edges are {@link TrackSegment}s. Supports
 * the two queries later phases lean on: "are A and B connected?" and "give me a
 * path between two connected points" (breadth-first, so shortest in hop count).
 */
public final class TrackNetwork {

    private final Set<TrackSegment> segments = new HashSet<TrackSegment>();
    private final Map<GridPoint, Set<GridPoint>> adjacency = new HashMap<GridPoint, Set<GridPoint>>();

    /**
     * Adds a segment between two adjacent tiles. No-op if it already exists.
     *
     * @return true if a new segment was added.
     */
    public boolean addSegment(TrackSegment seg) {
        if (!segments.add(seg)) {
            return false;
        }
        link(seg.getA(), seg.getB());
        link(seg.getB(), seg.getA());
        return true;
    }

    private void link(GridPoint from, GridPoint to) {
        Set<GridPoint> ns = adjacency.get(from);
        if (ns == null) {
            ns = new HashSet<GridPoint>();
            adjacency.put(from, ns);
        }
        ns.add(to);
    }

    public boolean hasSegmentBetween(GridPoint a, GridPoint b) {
        Set<GridPoint> ns = adjacency.get(a);
        return ns != null && ns.contains(b);
    }

    public boolean hasTrackAt(GridPoint p) {
        return adjacency.containsKey(p);
    }

    /** Unmodifiable view of every laid segment (for rendering). */
    public Set<TrackSegment> getSegments() {
        return Collections.unmodifiableSet(segments);
    }

    public int segmentCount() {
        return segments.size();
    }

    /** True if {@code b} is reachable from {@code a} along laid track. */
    public boolean isConnected(GridPoint a, GridPoint b) {
        if (a.equals(b)) {
            return adjacency.containsKey(a);
        }
        return findPath(a, b) != null;
    }

    public boolean areTownsConnected(Town a, Town b) {
        return isConnected(a.getPosition(), b.getPosition());
    }

    /**
     * Breadth-first search for a path of tiles from {@code start} to {@code goal}
     * over laid track. Returns the ordered list of points (inclusive of both
     * ends) or null if they are not connected.
     */
    public List<GridPoint> findPath(GridPoint start, GridPoint goal) {
        if (!adjacency.containsKey(start) || !adjacency.containsKey(goal)) {
            return null;
        }
        if (start.equals(goal)) {
            List<GridPoint> single = new ArrayList<GridPoint>();
            single.add(start);
            return single;
        }
        Map<GridPoint, GridPoint> cameFrom = new HashMap<GridPoint, GridPoint>();
        Set<GridPoint> visited = new HashSet<GridPoint>();
        Deque<GridPoint> queue = new ArrayDeque<GridPoint>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            GridPoint cur = queue.poll();
            if (cur.equals(goal)) {
                return reconstruct(cameFrom, start, goal);
            }
            for (GridPoint next : adjacency.get(cur)) {
                if (visited.add(next)) {
                    cameFrom.put(next, cur);
                    queue.add(next);
                }
            }
        }
        return null;
    }

    private List<GridPoint> reconstruct(Map<GridPoint, GridPoint> cameFrom, GridPoint start, GridPoint goal) {
        List<GridPoint> path = new ArrayList<GridPoint>();
        GridPoint cur = goal;
        while (cur != null && !cur.equals(start)) {
            path.add(cur);
            cur = cameFrom.get(cur);
        }
        path.add(start);
        Collections.reverse(path);
        return path;
    }
}
