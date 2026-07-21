package com.railroad.model;

import java.util.Collections;
import java.util.List;

/**
 * A fixed path a train runs along: an ordered list of tile points between two
 * endpoint towns. The train shuttles back and forth over this path. A route is
 * computed once (from the track network) when a train is assigned and stays
 * immutable thereafter.
 */
public final class Route {

    private final Town from;
    private final Town to;
    private final List<GridPoint> path; // inclusive of both town tiles

    public Route(Town from, Town to, List<GridPoint> path) {
        this.from = from;
        this.to = to;
        this.path = path;
    }

    public Town getFrom() {
        return from;
    }

    public Town getTo() {
        return to;
    }

    /** Ordered tiles from {@code from} to {@code to}, both inclusive. */
    public List<GridPoint> getPath() {
        return Collections.unmodifiableList(path);
    }

    /** Number of segments (path.size() - 1). Doubles as the trip's distance. */
    public int segmentCount() {
        return path.size() - 1;
    }
}
