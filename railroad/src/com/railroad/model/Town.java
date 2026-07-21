package com.railroad.model;

/**
 * A town on the map. Phase 1 only needs an id, a name and a tile position, but
 * fields for cargo supply/demand are stubbed as hooks so Phase 2 (stations,
 * production, demand) can extend this class rather than replace it.
 */
public final class Town {

    private final int id;
    private final String name;
    private final GridPoint position;

    // --- Hooks for later phases (unused in Phase 1) ---------------------------
    // Cargo supply/demand tables, population, and growth will live here. Left as
    // a comment rather than dead fields so Phase 1 stays lean.

    public Town(int id, String name, GridPoint position) {
        this.id = id;
        this.name = name;
        this.position = position;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public GridPoint getPosition() {
        return position;
    }

    public int getX() {
        return position.x;
    }

    public int getY() {
        return position.y;
    }
}
