package com.whim.tacticalnexus.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * One immutable floor. Cells default to empty (null); occupants are stored in a
 * sparse map keyed by {@link Position}. The {@code with}/{@code without}
 * mutators return new maps that structurally share an inexpensive copy of the
 * sparse occupant map, so snapshots for undo/redo are cheap.
 */
public final class GridMap {
    private final int rows;
    private final int cols;
    private final Map<Position, Entity> entities;

    public GridMap(int rows, int cols) {
        this(rows, cols, new HashMap<Position, Entity>());
    }

    private GridMap(int rows, int cols, Map<Position, Entity> entities) {
        this.rows = rows;
        this.cols = cols;
        this.entities = entities;
    }

    public int rows() {
        return rows;
    }

    public int cols() {
        return cols;
    }

    /** Returns the occupant at {@code p}, or null if the cell is empty floor. */
    public Entity at(Position p) {
        return entities.get(p);
    }

    public boolean inBounds(Position p) {
        return p.row() >= 0 && p.row() < rows && p.col() >= 0 && p.col() < cols;
    }

    /** Returns a NEW map with {@code e} placed at {@code p} (e==null clears the cell). */
    public GridMap with(Position p, Entity e) {
        Map<Position, Entity> copy = new HashMap<Position, Entity>(entities);
        if (e == null) {
            copy.remove(p);
        } else {
            copy.put(p, e);
        }
        return new GridMap(rows, cols, copy);
    }

    /** Convenience: returns a NEW map with {@code p} cleared. */
    public GridMap without(Position p) {
        return with(p, null);
    }

    /** Returns the position of the first stair travelling {@code dir}, or null. */
    public Position findStair(StairDirection dir) {
        for (Map.Entry<Position, Entity> entry : entities.entrySet()) {
            Entity e = entry.getValue();
            if (e instanceof Staircase && ((Staircase) e).direction() == dir) {
                return entry.getKey();
            }
        }
        return null;
    }
}
