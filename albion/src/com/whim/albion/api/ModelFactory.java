package com.whim.albion.api;

/**
 * Entry point the model/content package (Task 1) exposes so the engine (Task 2)
 * and the app wiring (orchestrator) can build a fresh game without importing any
 * concrete model classes. Implementations are stateless factories.
 */
public interface ModelFactory {

    /** Build a brand-new game model (starting party, starting map, content). */
    GameModel newGame(long seed);
}
