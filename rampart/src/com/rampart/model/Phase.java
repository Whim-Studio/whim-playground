package com.rampart.model;

/**
 * The lifecycle phase of a Rampart game. The core loop cycles
 * {@link #BUILD} &rarr; {@link #BATTLE} &rarr; {@link #REPAIR} and then either
 * advances via {@link #ROUND_TRANSITION} to the next {@link #BUILD} or ends at
 * {@link #GAME_OVER}.
 *
 * <p>The engine (Task 2) owns all phase transitions and timing; this enum is only
 * a label the model carries so the UI can render the right screen.
 */
public enum Phase {
    /** Attract / start screen before a game begins. */
    TITLE,
    /** Cannon-placement phase: the player seeds cannons inside enclosed territory. */
    BUILD,
    /** Battle phase: ships bombard walls and the player fires cannons. */
    BATTLE,
    /** Repair phase: the player drops Tetris-like wall pieces to re-seal loops. */
    REPAIR,
    /** Brief inter-round splash before the next BUILD phase. */
    ROUND_TRANSITION,
    /** The game has ended (no castle remained enclosed). */
    GAME_OVER
}
