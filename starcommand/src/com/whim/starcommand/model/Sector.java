package com.whim.starcommand.model;

import java.io.Serializable;

/**
 * One cell of the galaxy grid. Belongs to one of the three frontiers of
 * "The Triangle": the human core, the pirate Alpha Frontier, or the
 * insectoid Beta Frontier.
 */
public class Sector implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Frontier { CORE, ALPHA, BETA }

    public final int x;
    public final int y;
    public Frontier frontier;
    public Planet planet;      // may be null (empty space)
    public boolean visited = false;
    public boolean hostilePresence = false;

    public Sector(int x, int y, Frontier frontier) {
        this.x = x;
        this.y = y;
        this.frontier = frontier;
    }
}
