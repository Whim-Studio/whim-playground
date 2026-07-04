package com.rampart.model;

/**
 * Read-only snapshot of a single grid cell handed to the UI. Concrete class:
 * {@link Tile}. The UI must never cast this back to {@code Tile} or mutate it.
 */
public interface TileView {
    /** @return the column (x) of this cell */
    int col();

    /** @return the row (y) of this cell */
    int row();

    /** @return the terrain kind occupying this cell */
    TileType type();

    /**
     * @return {@code true} if the engine's last territory pass marked this cell as
     *         inside a sealed wall loop (enclosed land)
     */
    boolean enclosed();
}
