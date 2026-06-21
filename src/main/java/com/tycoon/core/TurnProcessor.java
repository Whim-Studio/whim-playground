package com.tycoon.core;

import java.util.List;

/**
 * The seam between core and sim. Task 1 declares it; Task 2 (com.tycoon.sim)
 * provides the implementation.
 */
public interface TurnProcessor {
    /**
     * Resolve exactly ONE in-game hour for player + all competitors, mutating
     * state. Return every Interrupt produced this hour (empty list = none).
     */
    List<Interrupt> processHour(GameState state);
}
