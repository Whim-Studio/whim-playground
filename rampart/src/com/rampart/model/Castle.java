package com.rampart.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A castle keep the player defends. Holds its grid position, alive/enclosed flags,
 * and the list of cells the engine's flood-fill last attributed to it as enclosed
 * territory. Pure state — the engine computes enclosure and writes it back here.
 */
public class Castle implements CastleView {
    private final Coord position;
    private boolean alive = true;
    private boolean enclosed;
    private final List<Coord> territory = new ArrayList<Coord>();

    /**
     * Creates a living castle at the given position.
     *
     * @param position the castle's grid cell (must be non-null)
     */
    public Castle(Coord position) {
        if (position == null) throw new IllegalArgumentException("position must not be null");
        this.position = position;
    }

    @Override public Coord position() { return position; }
    @Override public boolean alive() { return alive; }
    @Override public boolean enclosed() { return enclosed; }

    @Override
    public List<Coord> territory() {
        return Collections.unmodifiableList(territory);
    }

    @Override public int territorySize() { return territory.size(); }

    /**
     * Sets whether this castle is still standing (engine only).
     *
     * @param alive new alive state
     */
    public void setAlive(boolean alive) { this.alive = alive; }

    /**
     * Sets whether the engine's last pass found this castle enclosed (engine only).
     *
     * @param enclosed new enclosed state
     */
    public void setEnclosed(boolean enclosed) { this.enclosed = enclosed; }

    /**
     * Replaces this castle's enclosed-territory cell list (engine only). Copies the
     * supplied cells defensively.
     *
     * @param cells the enclosed cells; {@code null} is treated as empty
     */
    public void setTerritory(List<Coord> cells) {
        territory.clear();
        if (cells != null) territory.addAll(cells);
    }

    /** Empties the territory list and clears the enclosed flag (engine only). */
    public void clearTerritory() {
        territory.clear();
        enclosed = false;
    }

    @Override
    public String toString() {
        return "Castle" + position + (alive ? "" : ",dead") + (enclosed ? ",enclosed" : "")
                + ",territory=" + territory.size();
    }
}
