package com.whim.b5db.engine;

import java.util.List;

/**
 * Aggregate mutable state for one game: the seated players, the shared market,
 * the deterministic RNG, and the win threshold. Turn bookkeeping lives here so
 * the UI and the headless simulator drive the exact same engine.
 */
public final class GameState {

    private final List<PlayerState> players;
    private final Market market;
    private final Rng rng;
    private final int prestigeTarget;

    private int currentPlayer;
    private int turn;

    public GameState(List<PlayerState> players, Market market, Rng rng, int prestigeTarget) {
        this.players = players;
        this.market = market;
        this.rng = rng;
        this.prestigeTarget = prestigeTarget;
    }

    /** Deep copy with a fresh RNG, used for Monte-Carlo rollouts. */
    public GameState copy(Rng rolloutRng) {
        List<PlayerState> copies = new java.util.ArrayList<>();
        for (PlayerState p : players) {
            copies.add(new PlayerState(p));
        }
        GameState g = new GameState(copies, new Market(market), rolloutRng, prestigeTarget);
        g.currentPlayer = this.currentPlayer;
        g.turn = this.turn;
        return g;
    }

    public List<PlayerState> players() { return players; }
    public Market market() { return market; }
    public Rng rng() { return rng; }
    public int prestigeTarget() { return prestigeTarget; }

    public int currentPlayerIndex() { return currentPlayer; }
    public PlayerState current() { return players.get(currentPlayer); }
    public int turn() { return turn; }

    /** Advance to the next seat, incrementing the turn counter on wrap. */
    public void advance() {
        currentPlayer = (currentPlayer + 1) % players.size();
        if (currentPlayer == 0) {
            turn++;
        }
    }

    /** @return the leading player by PRESTIGE (ties broken by seat order). */
    public PlayerState leader() {
        PlayerState best = players.get(0);
        for (PlayerState p : players) {
            if (p.prestige() > best.prestige()) {
                best = p;
            }
        }
        return best;
    }
}
