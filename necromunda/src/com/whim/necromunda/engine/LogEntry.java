package com.whim.necromunda.engine;

/**
 * One line in the action/dice log, tagged with the turn number and phase it
 * happened in so the UI can render context. Immutable.
 */
public final class LogEntry {

    private final int turn;
    private final Phase phase;
    private final String message;

    public LogEntry(int turn, Phase phase, String message) {
        this.turn = turn;
        this.phase = phase;
        this.message = message;
    }

    public int turn() { return turn; }
    public Phase phase() { return phase; }
    public String message() { return message; }

    @Override
    public String toString() {
        return "T" + turn + " [" + phase.label() + "] " + message;
    }
}
