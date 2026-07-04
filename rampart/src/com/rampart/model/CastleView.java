package com.rampart.model;

import java.util.List;

/**
 * Read-only snapshot of a castle keep. Concrete class: {@link Castle}. The
 * enclosed-cell list is the territory the engine's flood-fill attributed to this
 * castle on its last pass.
 */
public interface CastleView {
    /** @return the castle's grid position */
    Coord position();

    /** @return {@code true} while the castle keep has not been destroyed */
    boolean alive();

    /** @return {@code true} if the engine's last pass found this castle fully enclosed */
    boolean enclosed();

    /**
     * @return an unmodifiable view of the cells the engine attributed as this
     *         castle's enclosed territory (empty if not enclosed)
     */
    List<Coord> territory();

    /** @return number of enclosed territory cells (size of {@link #territory()}) */
    int territorySize();
}
