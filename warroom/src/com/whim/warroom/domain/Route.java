package com.whim.warroom.domain;

import java.util.ArrayList;
import java.util.List;

/** An ordered list of {@link Waypoint}s a unit follows. */
public class Route {
    private final List<Waypoint> waypoints = new ArrayList<Waypoint>();

    public Route() {
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public void add(Waypoint w) {
        waypoints.add(w);
    }

    public boolean isEmpty() {
        return waypoints.isEmpty();
    }

    /** Arrival tick of the last waypoint, or 0 when the route is empty. */
    public int finalArrivalTick() {
        if (waypoints.isEmpty()) {
            return 0;
        }
        return waypoints.get(waypoints.size() - 1).getArrivalTick();
    }
}
