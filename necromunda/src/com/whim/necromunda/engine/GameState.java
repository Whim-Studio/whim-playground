package com.whim.necromunda.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.whim.necromunda.model.Gang;
import com.whim.necromunda.model.board.Board;

/**
 * The single source of truth for a battle in progress: the board, the two gangs,
 * which player is active, the current {@link Phase}, the turn number, the seeded
 * {@link Dice}, and an append-only action log.
 *
 * <p>Pure engine state — no Swing/AWT. Views observe changes via a lightweight
 * listener callback so the controller/UI can repaint without the engine knowing
 * anything about the UI.
 */
public final class GameState {

    private final Board board;
    private final List<Gang> gangs = new ArrayList<Gang>();
    private final Dice dice;

    private int activePlayerIndex;
    private Phase phase = Phase.RECOVERY;
    private int turnNumber = 1;

    private final List<LogEntry> log = new ArrayList<LogEntry>();
    private final List<Runnable> listeners = new ArrayList<Runnable>();

    public GameState(Board board, Gang gangA, Gang gangB, long seed) {
        this.board = board;
        this.gangs.add(gangA);
        this.gangs.add(gangB);
        this.dice = new Dice(seed);
    }

    public Board board() { return board; }
    public Dice dice() { return dice; }

    public List<Gang> gangs() { return gangs; }
    public Gang activeGang() { return gangs.get(activePlayerIndex); }
    public Gang inactiveGang() { return gangs.get(1 - activePlayerIndex); }
    public int activePlayerIndex() { return activePlayerIndex; }
    public void setActivePlayerIndex(int index) { this.activePlayerIndex = index; }

    public Phase phase() { return phase; }
    public void setPhase(Phase phase) { this.phase = phase; }

    public int turnNumber() { return turnNumber; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }

    // ------------------------------------------------------------------- log

    public List<LogEntry> log() {
        return Collections.unmodifiableList(log);
    }

    /** Append a log line tagged with the current turn and phase. */
    public void log(String message) {
        log.add(new LogEntry(turnNumber, phase, message));
    }

    // -------------------------------------------------------------- observers

    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    /** Notify all observers that state changed (they typically repaint). */
    public void fireChanged() {
        for (Runnable r : listeners) {
            r.run();
        }
    }
}
