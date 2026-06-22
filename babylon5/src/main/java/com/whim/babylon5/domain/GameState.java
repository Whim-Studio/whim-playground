package com.whim.babylon5.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The full mutable state of a game: the list of players, whose turn it is, the
 * current {@link Phase}, the turn counter, and a single deterministic RNG keyed
 * on the game seed. All randomness in the engine must flow through
 * {@link #getRng()} so a given seed always produces an identical game.
 */
public final class GameState {

    private final List<PlayerState> players;
    private final long seed;
    private final Random rng;

    private int activeIndex = 0;
    private Phase phase = Phase.READY;
    private int turn = 1;

    public GameState(List<PlayerState> players, long seed) {
        this.players = Collections.unmodifiableList(
                new ArrayList<PlayerState>(players == null ? new ArrayList<PlayerState>() : players));
        this.seed = seed;
        this.rng = new Random(seed);
    }

    public List<PlayerState> getPlayers() { return players; }

    public PlayerState getActivePlayer() {
        if (players.isEmpty()) return null;
        return players.get(activeIndex);
    }

    public int getActiveIndex() { return activeIndex; }
    public void setActiveIndex(int i) { this.activeIndex = i; }

    public Phase getPhase() { return phase; }
    public void setPhase(Phase p) { this.phase = p; }

    public int getTurn() { return turn; }
    public void incrementTurn() { this.turn++; }

    /** The deterministic, seeded RNG for this game. */
    public Random getRng() { return rng; }

    public long getSeed() { return seed; }
}
