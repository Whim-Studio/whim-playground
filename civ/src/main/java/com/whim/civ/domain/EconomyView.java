package com.whim.civ.domain;

/**
 * Read-only view the UI uses to display engine-computed economy numbers without importing
 * the engine package. Declared in the DOMAIN so both UI and engine can reference it without
 * a UI -> engine dependency. Task 2 (or the orchestrator Main) provides an adapter backed by
 * EconomyEngine.
 */
public interface EconomyView {
    int food(GameState s, City c);
    int shields(GameState s, City c);
    int trade(GameState s, City c);
    int[] tradeSplit(Civilization civ, int totalTrade);
}
