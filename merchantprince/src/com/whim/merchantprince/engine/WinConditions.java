package com.whim.merchantprince.engine;

import com.whim.merchantprince.model.Family;
import com.whim.merchantprince.model.GameState;
import com.whim.merchantprince.model.Good;
import com.whim.merchantprince.model.Office;
import com.whim.merchantprince.model.TransportUnit;

/**
 * Victory and scoring (GAME_DESIGN_REFERENCE §7): first family to
 * {@link Constants#WIN_FLORINS} wins immediately; otherwise the highest net worth
 * at the end year wins. Net worth counts florins, the market value of cargo and
 * ships, and — the confirmed rule — the value of bribed senators and cardinals
 * plus held offices.
 *
 * <p>Contract frozen for T0. Full net-worth valuation to be completed by the Politics task (T2).
 */
public final class WinConditions {
    private WinConditions() { }

    /** Total net worth of a family in florins (the end-game score). */
    public static long netWorth(GameState s, Family f) {
        long worth = f.florins;
        // Cargo + ships (rough liquidation value).
        for (TransportUnit u : s.unitsOf(f.id)) {
            worth += u.type.cost / 2;
            for (Good g : Good.ALL) worth += (long) u.cargo[g.ordinal()] * g.nominalValue;
        }
        // Offices + bribed officials (confirmed to count toward net worth).
        for (Office o : f.offices) worth += o.netWorth;
        worth += (long) f.senatorsBribed * Office.MINISTER.netWorth;
        worth += (long) f.cardinalsBribed * Office.CARDINAL.netWorth;
        return worth;
    }

    /** Set gameOver/victory on the state if a win or end condition is met. */
    public static void checkVictory(GameState s) {
        // TODO(T2): also handle player elimination / bankruptcy defeat.
        for (Family f : s.families) {
            if (f.florins >= Constants.WIN_FLORINS) {
                s.gameOver = true;
                s.victory = (f.id == s.playerId);
                s.gameOverReason = f.surname + " amassed one million florins!";
                return;
            }
        }
        if (s.year >= s.endYear) {
            Family best = null;
            long bestWorth = Long.MIN_VALUE;
            for (Family f : s.families) {
                long w = netWorth(s, f);
                if (w > bestWorth) { bestWorth = w; best = f; }
            }
            s.gameOver = true;
            s.victory = (best != null && best.id == s.playerId);
            s.gameOverReason = (best == null ? "" : "House " + best.surname
                    + " ends the era wealthiest (" + bestWorth + " florins).");
        }
    }
}
