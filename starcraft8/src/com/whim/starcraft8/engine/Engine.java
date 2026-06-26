package com.whim.starcraft8.engine;

import com.whim.starcraft8.domain.GameState;

/**
 * Factory for the simulation. {@code humanPlayerId} names the one player the UI
 * controls; every other player in the {@link GameState} is driven by the built-in AI.
 * Pass {@code -1} for a fully headless AI-vs-AI match (see {@code EngineSmokeTest}).
 */
public final class Engine {
    private Engine() {}

    public static Simulation create(GameState state, int humanPlayerId) {
        return new SimulationImpl(state, humanPlayerId);
    }
}
