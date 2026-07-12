package com.whim.firetop.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The full serializable snapshot of a game in progress: the board, the roster of
 * adventurers, whose turn it is, and terminal status. Decks are held here as raw
 * card lists so the whole state round-trips through serialization; the engine
 * wraps them in {@code Deck} at load time.
 */
public final class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Board board;
    private final List<Character> players;
    private int currentPlayerIndex;

    private final List<Card> encounterDeck;
    private final List<Card> treasureDeck;
    private final List<Card> eventDeck;

    private final List<String> log = new ArrayList<String>();

    private boolean gameOver;
    private boolean victory;
    private long seed;

    public GameState(Board board, List<Character> players,
                     List<Card> encounterDeck, List<Card> treasureDeck, List<Card> eventDeck,
                     long seed) {
        this.board = board;
        this.players = players;
        this.encounterDeck = encounterDeck;
        this.treasureDeck = treasureDeck;
        this.eventDeck = eventDeck;
        this.seed = seed;
    }

    public Board getBoard() { return board; }
    public List<Character> getPlayers() { return players; }
    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
    public void setCurrentPlayerIndex(int i) { this.currentPlayerIndex = i; }

    public List<Card> getEncounterDeck() { return encounterDeck; }
    public List<Card> getTreasureDeck() { return treasureDeck; }
    public List<Card> getEventDeck() { return eventDeck; }

    public List<String> getLog() { return log; }

    public boolean isGameOver() { return gameOver; }
    public void setGameOver(boolean gameOver) { this.gameOver = gameOver; }
    public boolean isVictory() { return victory; }
    public void setVictory(boolean victory) { this.victory = victory; }

    public long getSeed() { return seed; }

    public Character currentPlayer() { return players.get(currentPlayerIndex); }

    /** True if at least one adventurer is still alive. */
    public boolean anyAlive() {
        for (Character c : players) {
            if (c.isAlive()) {
                return true;
            }
        }
        return false;
    }
}
