package com.rampart.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The full mutable snapshot of a game in progress: the current {@link Phase}, round
 * number, timers, score/lives, territory reading, the {@link Grid}, and the live
 * lists of {@link Castle}s, {@link Cannon}s and {@link Ship}s plus the current and
 * queued {@link WallPiece}s.
 *
 * <p>Implements {@link GameStateView} so the engine can hand it straight to the UI
 * as a read-only snapshot. This class stores state and offers plain
 * getters/setters/list-mutators only; ALL rules, timers, AI, territory math and
 * transitions live in the engine (Task 2).
 */
public class GameState implements GameStateView {
    private Phase phase = Phase.TITLE;
    private int round = 1;
    private long timerRemainingMillis;
    private long score;
    private int lives;
    private double territoryFraction;
    private int cannonsRemainingToPlace;

    private final Grid grid;
    private final List<Castle> castles = new ArrayList<Castle>();
    private final List<Cannon> cannons = new ArrayList<Cannon>();
    private final List<Ship> ships = new ArrayList<Ship>();
    private final List<WallPiece> queuedPieces = new ArrayList<WallPiece>();
    private WallPiece currentPiece;

    /**
     * Creates a game state wrapping the given grid.
     *
     * @param grid the playfield grid (must be non-null)
     */
    public GameState(Grid grid) {
        if (grid == null) throw new IllegalArgumentException("grid must not be null");
        this.grid = grid;
    }

    // ---- GameStateView ----
    @Override public Phase phase() { return phase; }
    @Override public int round() { return round; }
    @Override public long timerRemainingMillis() { return timerRemainingMillis; }
    @Override public long score() { return score; }
    @Override public int lives() { return lives; }
    @Override public double territoryFraction() { return territoryFraction; }
    @Override public int cannonsRemainingToPlace() { return cannonsRemainingToPlace; }
    @Override public GridView grid() { return grid; }
    @Override public WallPieceView currentPiece() { return currentPiece; }
    @Override public boolean gameOver() { return phase == Phase.GAME_OVER; }

    @Override
    public List<? extends CastleView> castles() {
        return Collections.unmodifiableList(castles);
    }

    @Override
    public List<? extends CannonView> cannons() {
        return Collections.unmodifiableList(cannons);
    }

    @Override
    public List<? extends ShipView> ships() {
        return Collections.unmodifiableList(ships);
    }

    @Override
    public List<? extends WallPieceView> queuedPieces() {
        return Collections.unmodifiableList(queuedPieces);
    }

    @Override
    public int enclosedCastleCount() {
        int n = 0;
        for (int i = 0; i < castles.size(); i++) {
            Castle c = castles.get(i);
            if (c.alive() && c.enclosed()) n++;
        }
        return n;
    }

    // ---- Concrete accessors for the engine ----

    /** @return the concrete grid (engine convenience) */
    public Grid gridModel() { return grid; }

    /** @return the live, mutable castle list (engine only) */
    public List<Castle> castleList() { return castles; }

    /** @return the live, mutable cannon list (engine only) */
    public List<Cannon> cannonList() { return cannons; }

    /** @return the live, mutable ship list (engine only) */
    public List<Ship> shipList() { return ships; }

    /** @return the live, mutable queued-piece list (engine only) */
    public List<WallPiece> queuedPieceList() { return queuedPieces; }

    /** @return the concrete current piece, or {@code null} */
    public WallPiece currentPieceModel() { return currentPiece; }

    // ---- Setters (engine only) ----

    /** @param phase the new phase (must be non-null) */
    public void setPhase(Phase phase) {
        if (phase == null) throw new IllegalArgumentException("phase must not be null");
        this.phase = phase;
    }

    /** @param round the new 1-based round number (must be &ge; 1) */
    public void setRound(int round) {
        if (round < 1) throw new IllegalArgumentException("round must be >= 1");
        this.round = round;
    }

    /** @param millis remaining time in the current phase (clamped at zero) */
    public void setTimerRemainingMillis(long millis) {
        this.timerRemainingMillis = Math.max(0L, millis);
    }

    /** @param score the new score */
    public void setScore(long score) { this.score = score; }

    /**
     * Adds a signed delta to the score (score never drops below zero).
     *
     * @param delta points to add (may be negative)
     */
    public void addScore(long delta) {
        this.score = Math.max(0L, this.score + delta);
    }

    /** @param lives the new life count */
    public void setLives(int lives) { this.lives = lives; }

    /** @param fraction enclosed-land fraction in {@code [0,1]} (clamped) */
    public void setTerritoryFraction(double fraction) {
        if (fraction < 0.0) fraction = 0.0;
        if (fraction > 1.0) fraction = 1.0;
        this.territoryFraction = fraction;
    }

    /** @param n cannons still available to place this BUILD phase (clamped at zero) */
    public void setCannonsRemainingToPlace(int n) {
        this.cannonsRemainingToPlace = Math.max(0, n);
    }

    /** @param piece the piece now being placed, or {@code null} for none */
    public void setCurrentPiece(WallPiece piece) { this.currentPiece = piece; }

    @Override
    public String toString() {
        return "GameState(phase=" + phase + ",round=" + round + ",score=" + score
                + ",castles=" + castles.size() + ",cannons=" + cannons.size()
                + ",ships=" + ships.size() + ")";
    }
}
