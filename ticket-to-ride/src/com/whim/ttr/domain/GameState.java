package com.whim.ttr.domain;

import com.whim.ttr.api.GamePhase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The single shared, mutable game aggregate. The engine mutates it; the UI only
 * reads it. It bundles the board, the deck, the seated players, and the turn/
 * phase bookkeeping plus a human-readable status line for the dashboard.
 */
public final class GameState {

    private final Board board;
    private final Deck deck;
    private final List<Player> players;

    private int currentPlayerId = 0;
    private GamePhase phase = GamePhase.SETUP;
    private String lastMessage = "";

    public GameState(List<Player> players, Board board, Deck deck) {
        this.players = new ArrayList<Player>(players);
        this.board = board;
        this.deck = deck;
    }

    public Board board() { return board; }
    public Deck deck() { return deck; }

    public List<Player> players() { return Collections.unmodifiableList(players); }

    /** The player seated at {@code id}, or null if there is no such seat. */
    public Player player(int id) {
        for (Player p : players) {
            if (p.id() == id) return p;
        }
        return null;
    }

    public int playerCount() { return players.size(); }

    public int currentPlayerId() { return currentPlayerId; }
    public void setCurrentPlayerId(int id) { this.currentPlayerId = id; }

    /** Convenience: the {@link Player} whose turn it currently is. */
    public Player currentPlayer() { return player(currentPlayerId); }

    public GamePhase phase() { return phase; }
    public void setPhase(GamePhase p) { this.phase = p; }

    public String lastMessage() { return lastMessage; }
    public void setLastMessage(String m) { this.lastMessage = m == null ? "" : m; }
}
