package com.whim.tippingpoint.engine;

import com.whim.tippingpoint.domain.Player;

/**
 * Snapshot of the win/loss evaluation returned by {@link GameEngine#checkStatus()}.
 * {@link #getWinner()} is non-null only when the outcome is {@link Outcome#PLAYER_WIN}.
 */
public final class WinStatus {
    private final Outcome outcome;
    private final Player winner;
    private final String message;

    public WinStatus(Outcome outcome, Player winner, String message) {
        this.outcome = outcome;
        this.winner = winner;
        this.message = message;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    /** The winning player; null unless the outcome is {@link Outcome#PLAYER_WIN}. */
    public Player getWinner() {
        return winner;
    }

    public String getMessage() {
        return message;
    }

    public boolean isOver() {
        return outcome != Outcome.IN_PROGRESS;
    }
}
