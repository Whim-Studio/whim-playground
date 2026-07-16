package com.whim.tippingpoint.domain;

import java.util.ArrayList;
import java.util.List;

/** Mutable container for the full state of a game. Rules are enforced by the engine. */
public final class GameState {
    private final List<Player> players;
    private final Market market;
    private final TimelineTrack timeline;
    private final GameMode mode;
    private final List<WeatherCard> weatherDeck;
    private final List<WeatherCard> revealedWeather = new ArrayList<WeatherCard>();
    private Phase phase = Phase.DEVELOPMENT;
    private int currentPlayerIndex = 0;
    private int round = 1;

    public GameState(List<Player> players, Market market, TimelineTrack timeline,
                     List<WeatherCard> weatherDeck, GameMode mode) {
        this.players = players;
        this.market = market;
        this.timeline = timeline;
        this.weatherDeck = weatherDeck;
        this.mode = mode;
    }

    public List<Player> getPlayers() { return players; }
    public Market getMarket() { return market; }
    public TimelineTrack getTimeline() { return timeline; }
    public GameMode getMode() { return mode; }
    public List<WeatherCard> getWeatherDeck() { return weatherDeck; }
    public List<WeatherCard> getRevealedWeather() { return revealedWeather; }

    public int getGlobalCo2() {
        int sum = 0;
        for (int i = 0; i < players.size(); i++) {
            sum += players.get(i).getBoard().getCo2();
        }
        return sum;
    }

    public Phase getPhase() { return phase; }
    public void setPhase(Phase p) { this.phase = p; }

    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
    public void setCurrentPlayerIndex(int i) { this.currentPlayerIndex = i; }
    public Player getCurrentPlayer() { return players.get(currentPlayerIndex); }

    public int getRound() { return round; }
    public void incrementRound() { round++; }
}
