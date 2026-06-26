package com.whim.starcraft8.engine;

import com.whim.starcraft8.domain.GameState;

/**
 * Read-only view handed to the UI inside {@link Simulation#readState}. The returned
 * {@link GameState} is the live engine state and is ONLY valid while the consumer is
 * executing (the engine holds its lock for that duration).
 */
public interface WorldReader {
    GameState state();
}
