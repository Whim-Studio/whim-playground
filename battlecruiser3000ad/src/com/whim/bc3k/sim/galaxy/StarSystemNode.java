package com.whim.bc3k.sim.galaxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One star system on the navigation map: a position, a set of jump links to other
 * systems, whether it hosts a starstation, and whether the player has visited it.
 */
public final class StarSystemNode {
    private final int id;
    private final String name;
    private final int x, y;
    private final boolean hasStation;
    private final List<Integer> links = new ArrayList<Integer>();
    private boolean visited;

    public StarSystemNode(int id, String name, int x, int y, boolean hasStation) {
        this.id = id; this.name = name; this.x = x; this.y = y; this.hasStation = hasStation;
    }

    public int id() { return id; }
    public String name() { return name; }
    public int x() { return x; }
    public int y() { return y; }
    public boolean hasStation() { return hasStation; }
    public boolean visited() { return visited; }
    public void markVisited() { visited = true; }

    public List<Integer> links() { return Collections.unmodifiableList(links); }
    void link(int otherId) { if (!links.contains(otherId)) links.add(otherId); }
}
