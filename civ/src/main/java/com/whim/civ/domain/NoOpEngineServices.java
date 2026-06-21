package com.whim.civ.domain;

/**
 * Ships in domain so the game can run without the engine. Every method is a safe no-op.
 */
public final class NoOpEngineServices implements EngineServices {
    @Override
    public void runUpkeep(GameState state, Civilization civ) { /* no-op */ }

    @Override
    public void runProduction(GameState state, Civilization civ) { /* no-op */ }

    @Override
    public void runResearch(GameState state, Civilization civ) { /* no-op */ }

    @Override
    public void runAI(GameState state, Civilization civ) { /* no-op */ }
}
