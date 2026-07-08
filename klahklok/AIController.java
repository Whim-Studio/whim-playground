package klahklok;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Heuristic computer opponent for Klah Klok.
 *
 * Inspects a computer player's bankroll and places one to three randomized,
 * "logical" bets via {@link Player#placeBet(Symbol, int)}. It deliberately
 * varies its strategy from round to round so it does not feel mechanical, but
 * stays within sane risk limits.
 *
 * Guard rails:
 *   - never bets if bankroll <= 0
 *   - minimum individual bet of $10 (skips a bet it cannot afford)
 *   - never wagers more than ~30% of bankroll in total across all bets
 *   - all bets are whole dollars
 */
public class AIController {

    /** Smallest bet the AI is willing to place. */
    private static final int MIN_BET = 10;

    /** Hard cap on total wager as a fraction of bankroll. */
    private static final double MAX_TOTAL_FRACTION = 0.30;

    private final Random random;

    public AIController(Random random) {
        this.random = random;
    }

    /**
     * Assesses the player's bankroll and places a randomized set of bets.
     * Does nothing if the player is broke or too poor to meet the minimum bet.
     */
    public void placeBets(Player computer) {
        int bankroll = computer.getBankroll();
        if (bankroll <= 0 || bankroll < MIN_BET) {
            return; // broke or can't afford even the minimum — sit this round out
        }

        // Pick one of three broad strategies at random.
        int strategy = random.nextInt(3);
        switch (strategy) {
            case 0:
                spreadThreeSmallBets(computer, bankroll);
                break;
            case 1:
                oneLargerBet(computer, bankroll);
                break;
            default:
                twoMediumBets(computer, bankroll);
                break;
        }
    }

    /**
     * Strategy (a): three small bets (~3-5% of bankroll each) spread across
     * three distinct random symbols. Total stays well under the 30% cap.
     */
    private void spreadThreeSmallBets(Player computer, int bankroll) {
        List<Symbol> symbols = distinctRandomSymbols(3);
        int totalWagered = 0;
        int maxTotal = (int) (bankroll * MAX_TOTAL_FRACTION);

        for (Symbol symbol : symbols) {
            double fraction = 0.03 + random.nextDouble() * 0.02; // 3% - 5%
            int bet = roundToDollars(bankroll * fraction);
            bet = Math.max(bet, MIN_BET);

            if (bet < MIN_BET || totalWagered + bet > maxTotal) {
                continue; // skip bets we cannot afford within the cap
            }
            computer.placeBet(symbol, bet);
            totalWagered += bet;
        }
    }

    /**
     * Strategy (b): a single larger bet (~10-15% of bankroll) on one symbol.
     */
    private void oneLargerBet(Player computer, int bankroll) {
        Symbol symbol = randomSymbol();
        double fraction = 0.10 + random.nextDouble() * 0.05; // 10% - 15%
        int bet = roundToDollars(bankroll * fraction);
        bet = Math.max(bet, MIN_BET);

        int maxTotal = (int) (bankroll * MAX_TOTAL_FRACTION);
        if (bet < MIN_BET) {
            return;
        }
        if (bet > maxTotal) {
            bet = maxTotal; // respect the overall cap
        }
        if (bet >= MIN_BET) {
            computer.placeBet(symbol, bet);
        }
    }

    /**
     * Strategy (c): two medium bets (~7-10% of bankroll each) on two distinct
     * symbols, kept under the 30% total cap.
     */
    private void twoMediumBets(Player computer, int bankroll) {
        List<Symbol> symbols = distinctRandomSymbols(2);
        int totalWagered = 0;
        int maxTotal = (int) (bankroll * MAX_TOTAL_FRACTION);

        for (Symbol symbol : symbols) {
            double fraction = 0.07 + random.nextDouble() * 0.03; // 7% - 10%
            int bet = roundToDollars(bankroll * fraction);
            bet = Math.max(bet, MIN_BET);

            if (bet < MIN_BET || totalWagered + bet > maxTotal) {
                continue;
            }
            computer.placeBet(symbol, bet);
            totalWagered += bet;
        }
    }

    /** Rounds a dollar amount to the nearest whole dollar. */
    private static int roundToDollars(double amount) {
        return (int) Math.round(amount);
    }

    /** Returns a single random symbol. */
    private Symbol randomSymbol() {
        Symbol[] all = Symbol.values();
        return all[random.nextInt(all.length)];
    }

    /**
     * Returns {@code count} distinct symbols chosen at random (clamped to the
     * number of symbols that exist).
     */
    private List<Symbol> distinctRandomSymbols(int count) {
        List<Symbol> pool = new ArrayList<Symbol>();
        for (Symbol symbol : Symbol.values()) {
            pool.add(symbol);
        }
        Collections.shuffle(pool, random);

        int n = Math.min(count, pool.size());
        return new ArrayList<Symbol>(pool.subList(0, n));
    }
}
