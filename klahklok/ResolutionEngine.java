package klahklok;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves a roll of the three Klah Klok (Hi-Lo) dice against every player's
 * active bets, applying bankroll changes and reporting the net delta per
 * player per symbol.
 *
 * Payout rules (per active bet on symbol S with amount A, where c = number of
 * the 3 dice showing S):
 *   c == 0 -> lose the wager:      adjustBankroll(-A)
 *   c == 1 -> 1:1 (stake + 1x):    adjustBankroll(+1 * A)
 *   c == 2 -> 2:1 (stake + 2x):    adjustBankroll(+2 * A)
 *   c == 3 -> 3:1 (stake + 3x):    adjustBankroll(+3 * A)
 *
 * Note: bankroll is only ever changed here at resolution time, never at bet time.
 */
public class ResolutionEngine {

    /**
     * Applies bankroll changes to every player for the given roll and returns
     * the net delta per player, broken down per symbol. After applying deltas,
     * every player's bets are cleared.
     *
     * @param players the players whose bets should be resolved
     * @param roll    the three dice symbols that came up
     * @return map of player -> (symbol -> net delta applied for that bet)
     */
    public Map<Player, Map<Symbol, Integer>> resolve(List<Player> players, Symbol[] roll) {
        Map<Player, Map<Symbol, Integer>> result = new HashMap<Player, Map<Symbol, Integer>>();

        for (Player player : players) {
            Map<Symbol, Integer> perSymbol = new HashMap<Symbol, Integer>();

            // Copy the bets so we are not iterating a map we may indirectly mutate.
            @SuppressWarnings("unchecked")
            Map<Symbol, Integer> bets = new HashMap<Symbol, Integer>(player.getBets());

            for (Map.Entry<Symbol, Integer> entry : bets.entrySet()) {
                Symbol symbol = entry.getKey();
                int wager = entry.getValue();
                if (wager <= 0) {
                    continue; // ignore empty / non-positive bets
                }

                int count = countSymbol(roll, symbol);
                int delta = netDelta(wager, count);

                player.adjustBankroll(delta);
                perSymbol.put(symbol, delta);
            }

            result.put(player, perSymbol);
        }

        // Clear bets only after all deltas have been applied.
        for (Player player : players) {
            player.clearBets();
        }

        return result;
    }

    /**
     * Net bankroll change for a single bet given how many dice matched.
     * No side effects — handy for a UI to preview an outcome.
     *
     * count 0 -> -wager, otherwise +count * wager.
     */
    public static int netDelta(int wager, int count) {
        if (count <= 0) {
            return -wager;
        }
        return count * wager;
    }

    /** Counts how many of the three dice show the given symbol. */
    private static int countSymbol(Symbol[] roll, Symbol symbol) {
        int count = 0;
        for (Symbol die : roll) {
            if (die == symbol) {
                count++;
            }
        }
        return count;
    }

    /**
     * Convenience overload returning the full list of distinct symbols that
     * appear in a roll — not required by the contract but cheap and useful.
     */
    public static List<Symbol> distinctSymbols(Symbol[] roll) {
        List<Symbol> seen = new ArrayList<Symbol>();
        for (Symbol die : roll) {
            if (!seen.contains(die)) {
                seen.add(die);
            }
        }
        return seen;
    }
}
