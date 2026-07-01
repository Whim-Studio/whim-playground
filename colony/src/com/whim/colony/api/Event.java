package com.whim.colony.api;

import com.whim.colony.ColonyState;

/**
 * A storyteller incident that mutates the colony when it fires (a raid, a cold
 * snap, a resource drop). The engine (Task 2) decides WHEN to raise an Event and
 * calls {@link #apply}; the UI (Task 3) may surface {@link #describe()} in the
 * message log. Backing data typically lives in a
 * {@link com.whim.colony.domain.Incident}.
 */
public interface Event {

    /**
     * Apply this event's effects to the colony, mutating {@code state} (spawning
     * raiders, dropping the temperature, adding resources, etc.).
     *
     * @param state the shared colony state to mutate
     */
    void apply(ColonyState state);

    /**
     * @return a human-readable one-line description of the event for the message
     * log / UI. Never {@code null}.
     */
    String describe();
}
