package com.whim.ttr.domain;

import com.whim.ttr.api.CardColor;
import com.whim.ttr.api.GameConstants;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mutable per-seat player state. The engine owns all mutation; the UI only
 * reads. Starts with {@link GameConstants#TRAINS_PER_PLAYER} trains and
 * {@link GameConstants#STATIONS_PER_PLAYER} stations.
 */
public final class Player {

    private final int id;
    private final String name;
    private final Color token;

    private int trainsLeft = GameConstants.TRAINS_PER_PLAYER;
    private int stationsLeft = GameConstants.STATIONS_PER_PLAYER;
    private final List<CardColor> hand = new ArrayList<CardColor>();
    private final List<DestinationTicket> tickets = new ArrayList<DestinationTicket>();
    private final List<String> stationCities = new ArrayList<String>();
    private int score = 0;

    public Player(int id, String name, Color token) {
        this.id = id;
        this.name = name;
        this.token = token;
    }

    public int id() { return id; }
    public String name() { return name; }
    public Color token() { return token; }

    public int trainsLeft() { return trainsLeft; }
    public int stationsLeft() { return stationsLeft; }
    public int score() { return score; }

    /** Live view of the hand (engine mutates through the mutators below). */
    public List<CardColor> hand() { return hand; }
    public List<DestinationTicket> tickets() { return tickets; }
    public List<String> stationCities() { return Collections.unmodifiableList(stationCities); }

    // ---- mutators used by the engine ---------------------------------------

    public void addCard(CardColor c) {
        hand.add(c);
    }

    /**
     * Remove exactly {@code n} cards of color {@code c} from the hand.
     * Returns false and changes nothing if the hand holds fewer than {@code n}.
     */
    public boolean removeCards(CardColor c, int n) {
        int have = 0;
        for (CardColor h : hand) {
            if (h == c) have++;
        }
        if (have < n) return false;
        int removed = 0;
        for (int i = hand.size() - 1; i >= 0 && removed < n; i--) {
            if (hand.get(i) == c) {
                hand.remove(i);
                removed++;
            }
        }
        return true;
    }

    public void useTrains(int n) {
        trainsLeft -= n;
    }

    public void useStation(String cityId) {
        if (stationsLeft > 0) {
            stationsLeft--;
            stationCities.add(cityId);
        }
    }

    public void addTicket(DestinationTicket t) {
        tickets.add(t);
    }

    public void addScore(int pts) {
        score += pts;
    }

    /** Count of a single color currently in hand (convenience for the engine/UI). */
    public int countOf(CardColor c) {
        int n = 0;
        for (CardColor h : hand) {
            if (h == c) n++;
        }
        return n;
    }

    @Override public String toString() { return name + "#" + id; }
}
