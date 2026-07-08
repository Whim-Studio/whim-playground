package klahklok;

import java.util.*;

/**
 * Mutable state of a Klah Klok game: players, turn order, phase, last roll, and round.
 */
public class GameState {

    /** The phases of a single game round. */
    public enum Phase { BETTING, ROLLING, RESOLUTION, GAME_OVER }

    private final List<Player> players;
    private Phase phase;
    private int activeIndex;
    private Symbol[] lastRoll;
    private int roundNumber;

    /**
     * Creates a game state.
     *
     * @param players the participating players, in turn order
     */
    public GameState(List<Player> players) {
        this.players = players;
        this.phase = Phase.BETTING;
        this.activeIndex = 0;
        this.lastRoll = null;
        this.roundNumber = 1;
    }

    /** @return the players in turn order. */
    public List<Player> getPlayers() {
        return players;
    }

    /** @return the current phase. */
    public Phase getPhase() {
        return phase;
    }

    /**
     * Sets the current phase.
     *
     * @param phase the new phase
     */
    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    /** @return the player whose turn it currently is. */
    public Player getActivePlayer() {
        return players.get(activeIndex);
    }

    /** Advances the active player index cyclically. */
    public void nextPlayer() {
        activeIndex = (activeIndex + 1) % players.size();
    }

    /** @return the index of the active player. */
    public int getActiveIndex() {
        return activeIndex;
    }

    /** @return the three symbols from the last roll, or null before the first roll. */
    public Symbol[] getLastRoll() {
        return lastRoll;
    }

    /**
     * Records the most recent roll.
     *
     * @param roll a length-3 array of rolled symbols
     */
    public void setLastRoll(Symbol[] roll) {
        this.lastRoll = roll;
    }

    /** @return the current round number, starting at 1. */
    public int getRoundNumber() {
        return roundNumber;
    }

    /** Increments the round number. */
    public void incrementRound() {
        roundNumber++;
    }
}
