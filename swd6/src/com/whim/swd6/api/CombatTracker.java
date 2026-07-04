package com.whim.swd6.api;

import java.util.List;

/**
 * Turn-order and state manager for a single encounter. Implemented by the engine
 * layer (Task 2), consumed by the UI (Task 3). Holds an ordered list of combatants,
 * advances turns, and applies damage results to wound state.
 *
 * Owned by the orchestrator (api).
 */
public interface CombatTracker {

    /** Add a combatant to the encounter. */
    void add(Combatant c);

    /** Roll initiative for everyone and sort the order (highest first). */
    void rollInitiative();

    /** The current turn order (after {@link #rollInitiative()}). */
    List<Combatant> order();

    /** The combatant whose turn it is, or null if the encounter is over. */
    Combatant current();

    /** Advance to the next living combatant; wraps to a new round as needed. */
    void next();

    /** 1-based round counter. */
    int round();

    /**
     * Apply a resolved hit to a target, escalating its wound level, and return the
     * resulting level. Also removes/marks combatants that are Incapacitated or worse
     * from the active order.
     */
    WoundLevel applyHit(Combatant target, DamageResult result);

    /** True when only one side (or nobody) remains able to act. */
    boolean isOver();
}
