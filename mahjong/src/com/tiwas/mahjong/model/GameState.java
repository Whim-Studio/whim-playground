package com.tiwas.mahjong.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The mutable state of a game in progress: the wall, the four seated players,
 * the discard pile, whose turn it is, the prevailing round wind, the dealer, and
 * bookkeeping for the current hand.
 */
public final class GameState {

    private Wall wall;
    private final List<Player> players = new ArrayList<Player>();
    private final List<Tile> discards = new ArrayList<Tile>();

    private int currentTurn;      // index into players
    private Wind roundWind;
    private int dealer;           // index of the dealer (seat East)
    private int handNumber;       // 1..16 across the whole game
    private int limit = Constants.DEFAULT_LIMIT;

    private Tile lastDiscard;     // most recent discard, if it is still claimable
    private int lastDiscardBy = -1;
    private boolean firstGoAround; // true until the first discard of the hand happens
    private int tilesPlayed;       // discards + draws into play this hand

    public Wall getWall() {
        return wall;
    }

    public void setWall(Wall wall) {
        this.wall = wall;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Player getPlayer(int index) {
        return players.get(index);
    }

    public List<Tile> getDiscards() {
        return discards;
    }

    public int getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(int currentTurn) {
        this.currentTurn = currentTurn;
    }

    public Player getCurrentPlayer() {
        return players.get(currentTurn);
    }

    public Wind getRoundWind() {
        return roundWind;
    }

    public void setRoundWind(Wind roundWind) {
        this.roundWind = roundWind;
    }

    public int getDealer() {
        return dealer;
    }

    public void setDealer(int dealer) {
        this.dealer = dealer;
    }

    public int getHandNumber() {
        return handNumber;
    }

    public void setHandNumber(int handNumber) {
        this.handNumber = handNumber;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public Tile getLastDiscard() {
        return lastDiscard;
    }

    public int getLastDiscardBy() {
        return lastDiscardBy;
    }

    public void setLastDiscard(Tile tile, int by) {
        this.lastDiscard = tile;
        this.lastDiscardBy = by;
    }

    public void clearLastDiscard() {
        this.lastDiscard = null;
        this.lastDiscardBy = -1;
    }

    public boolean isFirstGoAround() {
        return firstGoAround;
    }

    public void setFirstGoAround(boolean firstGoAround) {
        this.firstGoAround = firstGoAround;
    }

    public int getTilesPlayed() {
        return tilesPlayed;
    }

    public void incTilesPlayed() {
        this.tilesPlayed++;
    }

    public void resetTilesPlayed() {
        this.tilesPlayed = 0;
    }

    /**
     * Play proceeds counter-clockwise; with seats ordered E,S,W,N clockwise that
     * means decreasing index (wrapping).
     */
    public int nextSeat(int seat) {
        return (seat + Constants.NUM_PLAYERS - 1) % Constants.NUM_PLAYERS;
    }

    /** Index of the human player, or -1 if none. */
    public int humanIndex() {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).isHuman()) {
                return i;
            }
        }
        return -1;
    }
}
