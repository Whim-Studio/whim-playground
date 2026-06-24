package com.dglz.engine;

import com.dglz.domain.Combination;
import com.dglz.domain.Play;
import com.dglz.domain.Player;
import com.dglz.domain.Road;
import com.dglz.domain.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Full mutable game snapshot. Mutated by GameEngine (same package). */
public final class GameState {
    private final List<Player> players;
    private int leaderSeat;
    private int currentSeat;
    private Road currentRoad;
    private Combination currentBest;
    private int currentBestSeat = -1;
    private final List<Play> trickPlays = new ArrayList<Play>();
    private boolean gameOver;
    private Team winningTeam;
    private final List<String> log = new ArrayList<String>();

    public GameState(List<Player> players) {
        this.players = players;
    }

    public List<Player> players() {
        return Collections.unmodifiableList(players);
    }

    public int leaderSeat() {
        return leaderSeat;
    }

    public int currentSeat() {
        return currentSeat;
    }

    public Road currentRoad() {
        return currentRoad;
    }

    public Combination currentBest() {
        return currentBest;
    }

    public int currentBestSeat() {
        return currentBestSeat;
    }

    public List<Play> trickPlays() {
        return Collections.unmodifiableList(trickPlays);
    }

    public boolean gameOver() {
        return gameOver;
    }

    public Team winningTeam() {
        return winningTeam;
    }

    public List<String> log() {
        return Collections.unmodifiableList(log);
    }

    // ---- package-private mutators used by GameEngine ----

    void setLeaderSeat(int seat) {
        this.leaderSeat = seat;
    }

    void setCurrentSeat(int seat) {
        this.currentSeat = seat;
    }

    void setCurrentRoad(Road road) {
        this.currentRoad = road;
    }

    void setCurrentBest(Combination combo) {
        this.currentBest = combo;
    }

    void setCurrentBestSeat(int seat) {
        this.currentBestSeat = seat;
    }

    void addTrickPlay(Play play) {
        this.trickPlays.add(play);
    }

    void clearTrickPlays() {
        this.trickPlays.clear();
    }

    void setGameOver(boolean over) {
        this.gameOver = over;
    }

    void setWinningTeam(Team team) {
        this.winningTeam = team;
    }

    void addLog(String line) {
        this.log.add(line);
    }

    Player playerAt(int seat) {
        return players.get(seat);
    }

    int playerCount() {
        return players.size();
    }
}
