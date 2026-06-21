package com.whim.startrek.domain;

/**
 * Empty {@link GameServices} implementation so the domain (and UI) can compile and
 * run standalone before Task 2 lands. The orchestrator swaps in Task 2's real
 * {@code EngineServices} during consolidation.
 */
public class NoOpGameServices implements GameServices {

    @Override
    public void applyIncome(GameState s) {
        // no-op
    }

    @Override
    public void applyResearch(GameState s) {
        // no-op
    }

    @Override
    public void resolveMovement(GameState s) {
        // no-op
    }

    @Override
    public void resolveCombat(GameState s) {
        // no-op
    }

    @Override
    public void stepBorg(GameState s) {
        // no-op
    }
}
