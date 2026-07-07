package com.whim.b5wars.engine;

/**
 * The phases of one full Turn, driven as a finite-state machine by {@link TurnManager}.
 *
 * <pre>INITIATIVE -> POWER -> EW -> IMPULSE -> END_OF_TURN -> (next turn)</pre>
 */
public enum TurnPhase {
    INITIATIVE,
    POWER,
    EW,
    IMPULSE,
    END_OF_TURN
}
