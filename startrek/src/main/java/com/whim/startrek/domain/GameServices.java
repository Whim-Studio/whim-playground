package com.whim.startrek.domain;

/**
 * Bridge interface so {@link TurnManager} can drive Task 2 (engine) logic without
 * the domain package importing concrete engine classes. Task 2 supplies the
 * implementation ({@code EngineServices}); Task 1 only depends on this interface.
 */
public interface GameServices {

    void applyIncome(GameState s);

    void applyResearch(GameState s);

    /** Fleet nav, wormhole/hazard effects, cloak updates. */
    void resolveMovement(GameState s);

    /** Auto-resolve TBS encounters not opened as live battles. */
    void resolveCombat(GameState s);

    void stepBorg(GameState s);
}
