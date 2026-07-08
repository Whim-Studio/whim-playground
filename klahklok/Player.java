package klahklok;

import java.util.*;

/**
 * A Klah Klok player (human or computer) with a bankroll and current bets.
 */
public class Player {

    private final String name;
    private int bankroll;
    private final boolean computer;
    private final Map<Symbol, Integer> bets;

    /**
     * Creates a player.
     *
     * @param name     display name
     * @param bankroll starting bankroll
     * @param computer true if this player is AI-controlled
     */
    public Player(String name, int bankroll, boolean computer) {
        this.name = name;
        this.bankroll = bankroll;
        this.computer = computer;
        this.bets = new LinkedHashMap<Symbol, Integer>();
    }

    /** @return the player's display name. */
    public String getName() {
        return name;
    }

    /** @return the current bankroll. */
    public int getBankroll() {
        return bankroll;
    }

    /**
     * Sets the bankroll to an absolute amount.
     *
     * @param amount the new bankroll value
     */
    public void setBankroll(int amount) {
        this.bankroll = amount;
    }

    /**
     * Adjusts the bankroll by a (possibly negative) delta.
     *
     * @param delta the amount to add to the bankroll
     */
    public void adjustBankroll(int delta) {
        this.bankroll += delta;
    }

    /** @return true if this player is computer-controlled. */
    public boolean isComputer() {
        return computer;
    }

    /** @return the live map of symbol to wagered amount. */
    public Map<Symbol, Integer> getBets() {
        return bets;
    }

    /**
     * Adds a wager on a symbol, accumulating onto any existing bet.
     *
     * @param symbol the symbol to bet on
     * @param amount the amount to add to the bet
     */
    public void placeBet(Symbol symbol, int amount) {
        Integer existing = bets.get(symbol);
        int current = existing == null ? 0 : existing.intValue();
        bets.put(symbol, current + amount);
    }

    /** Clears all current bets. */
    public void clearBets() {
        bets.clear();
    }

    /** @return the sum of all current bets. */
    public int getTotalWagered() {
        int total = 0;
        for (Integer amount : bets.values()) {
            total += amount.intValue();
        }
        return total;
    }
}
