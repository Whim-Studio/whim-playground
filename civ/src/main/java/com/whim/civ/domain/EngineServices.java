package com.whim.civ.domain;

/**
 * The bridge interface the domain calls into for engine behavior, so the domain never
 * depends on the engine package. Task 2 implements this; the domain ships a
 * {@link NoOpEngineServices} for standalone / unit testing.
 */
public interface EngineServices {
    void runUpkeep(GameState state, Civilization civ);        // food/upkeep/disorder
    void runProduction(GameState state, Civilization civ);    // shields -> units/buildings
    void runResearch(GameState state, Civilization civ);      // beakers -> new tech
    void runAI(GameState state, Civilization civ);            // no-op for human civ
}
