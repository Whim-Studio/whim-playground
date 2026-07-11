package com.whim.bc3k.sim.galaxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The navigable star map. Systems are connected by bidirectional jump links; the
 * player occupies one system at a time and may only jump to a directly linked
 * system. Deterministic — the default map is fixed — so navigation is unit-testable.
 *
 * Scale here is a deliberately small v1 slice of BC3K's galaxy (the original v2.0
 * had 25 systems); see BC3K_Phase1_Design.md §7.
 */
public final class Galaxy {

    private final List<StarSystemNode> systems = new ArrayList<StarSystemNode>();
    private int currentId;

    private Galaxy() {}

    /** Build the fixed default sector: a small connected cluster around GALCOM HQ. */
    public static Galaxy defaultSector() {
        Galaxy g = new Galaxy();
        g.add(new StarSystemNode(0, "Sol (GALCOM HQ)", 120, 360, true));
        g.add(new StarSystemNode(1, "Centauri",        320, 220, true));
        g.add(new StarSystemNode(2, "Sirius",          360, 480, false));
        g.add(new StarSystemNode(3, "Vega",            560, 300, true));
        g.add(new StarSystemNode(4, "Gammula Reach",   820, 340, false)); // frontier
        g.link(0, 1); g.link(0, 2); g.link(1, 3); g.link(2, 3); g.link(3, 4);
        g.currentId = 0;
        g.current().markVisited();
        return g;
    }

    private void add(StarSystemNode n) { systems.add(n); }
    private void link(int a, int b) { byId(a).link(b); byId(b).link(a); }

    public List<StarSystemNode> systems() { return Collections.unmodifiableList(systems); }
    public int currentId() { return currentId; }
    public StarSystemNode current() { return byId(currentId); }

    public StarSystemNode byId(int id) {
        for (StarSystemNode n : systems) if (n.id() == id) return n;
        return null;
    }

    /** True if the target is a directly linked, reachable system. */
    public boolean canJumpTo(int targetId) {
        return targetId != currentId && current().links().contains(targetId);
    }

    /** Jump to a linked system, marking it visited. Returns false for illegal jumps. */
    public boolean jumpTo(int targetId) {
        if (!canJumpTo(targetId)) return false;
        currentId = targetId;
        current().markVisited();
        return true;
    }

    /** Reachable neighbours from the current system. */
    public List<StarSystemNode> neighbours() {
        List<StarSystemNode> out = new ArrayList<StarSystemNode>();
        for (Integer id : current().links()) out.add(byId(id));
        return out;
    }
}
