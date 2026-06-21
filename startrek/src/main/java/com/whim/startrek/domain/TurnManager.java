package com.whim.startrek.domain;

/**
 * Runs the per-turn phase state machine. The engine (Task 2) is injected as
 * {@link GameServices} so the domain has no compile dependency on the engine.
 * {@link #advanceTurn()} runs INCOME -> RESEARCH -> MOVEMENT -> COMBAT -> BORG -> END
 * and then increments the turn number.
 */
public class TurnManager {

    private final GameState state;
    private final GameServices services;

    public TurnManager(GameState state, GameServices services) {
        this.state = state;
        this.services = services == null ? new NoOpGameServices() : services;
    }

    /** Runs all phases in order, then advances the turn counter. */
    public void advanceTurn() {
        state.setPhase(TurnPhase.INCOME);
        services.applyIncome(state);

        state.setPhase(TurnPhase.RESEARCH);
        services.applyResearch(state);

        state.setPhase(TurnPhase.MOVEMENT);
        services.resolveMovement(state);

        state.setPhase(TurnPhase.COMBAT);
        services.resolveCombat(state);

        state.setPhase(TurnPhase.BORG);
        services.stepBorg(state);

        state.setPhase(TurnPhase.END);
        state.setTurnNumber(state.getTurnNumber() + 1);
    }

    public TurnPhase getCurrentPhase() {
        return state.getPhase();
    }
}
