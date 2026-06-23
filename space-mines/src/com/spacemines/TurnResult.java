package com.spacemines;

/**
 * Outcome of resolving one turn (year). Produced by the engine and rendered
 * by the UI.
 */
public class TurnResult {

    /** Human-readable summary of everything that happened this turn. */
    public String narrative;

    /** True if the game has ended (last year reached, revolt, bankruptcy, etc.). */
    public boolean gameOver;

    /** If {@link #gameOver} is true, the reason the game ended; otherwise null. */
    public String gameOverReason;
}
