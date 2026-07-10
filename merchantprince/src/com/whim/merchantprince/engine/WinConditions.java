package com.whim.merchantprince.engine;

import com.whim.merchantprince.model.Family;
import com.whim.merchantprince.model.GameState;
import com.whim.merchantprince.model.Good;
import com.whim.merchantprince.model.Office;
import com.whim.merchantprince.model.TransportUnit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Victory and scoring (GAME_DESIGN_REFERENCE §7): first family to
 * {@link Constants#WIN_FLORINS} wins immediately; otherwise the highest net worth
 * at the end year wins. Net worth counts florins, the liquidation value of cargo and
 * ships, and — the confirmed rule — the value of bribed senators and cardinals plus
 * held offices.
 *
 * <p>Implemented by the Politics task (T2). Adds player defeat (bankruptcy /
 * elimination) and a net-worth ranking helper for the scoreboard.
 */
public final class WinConditions {
    private WinConditions() { }

    /** Below this liquid florin floor with no ships/cargo, a family is bankrupt. */
    private static final long BANKRUPTCY_FLORINS = 0L;

    /** Total net worth of a family in florins (the end-game score). */
    public static long netWorth(GameState s, Family f) {
        if (f.eliminated) return 0L; // a wiped-out house scores nothing
        long worth = f.florins;
        // Cargo + ships (rough liquidation value: hull at half cost, cargo at nominal).
        for (TransportUnit u : s.unitsOf(f.id)) {
            worth += u.type.cost / 2;
            for (Good g : Good.ALL) worth += (long) u.cargo[g.ordinal()] * g.nominalValue;
        }
        // Offices + bribed officials (confirmed to count toward net worth, §7).
        for (Office o : f.offices) worth += o.netWorth;
        worth += (long) f.senatorsBribed * Office.MINISTER.netWorth;
        worth += (long) f.cardinalsBribed * Office.CARDINAL.netWorth;
        return worth;
    }

    /** True if a family has no liquid florins and nothing left to liquidate. */
    public static boolean isBankrupt(GameState s, Family f) {
        if (f.eliminated) return true;
        if (f.florins > BANKRUPTCY_FLORINS) return false;
        // Any ship, cargo, office or bribed official keeps a family solvent.
        if (!s.unitsOf(f.id).isEmpty()) return false;
        if (!f.offices.isEmpty() || f.senatorsBribed > 0 || f.cardinalsBribed > 0) return false;
        return true;
    }

    /** A ranked scoreboard entry: a family and its computed net worth. */
    public static final class Score {
        public final Family family;
        public final long netWorth;
        Score(Family family, long netWorth) {
            this.family = family;
            this.netWorth = netWorth;
        }
    }

    /** All families ranked by net worth, richest first (for a scoreboard). */
    public static List<Score> ranking(GameState s) {
        List<Score> scores = new ArrayList<Score>();
        for (Family f : s.families) scores.add(new Score(f, netWorth(s, f)));
        Collections.sort(scores, new Comparator<Score>() {
            public int compare(Score a, Score b) {
                return Long.compare(b.netWorth, a.netWorth); // descending
            }
        });
        return scores;
    }

    /** Set gameOver/victory on the state if a win or end condition is met. */
    public static void checkVictory(GameState s) {
        Family player = s.player();

        // 1. Player defeat: the human family eliminated or bankrupt ends the game.
        if (player != null && (player.eliminated || isBankrupt(s, player))) {
            s.gameOver = true;
            s.victory = false;
            s.gameOverReason = player.eliminated
                    ? "House " + player.surname + " has been destroyed by its rivals."
                    : "House " + player.surname + " is bankrupt and ruined.";
            return;
        }

        // 2. Instant win: first family (still in play) to reach the florin threshold.
        for (Family f : s.families) {
            if (!f.eliminated && f.florins >= Constants.WIN_FLORINS) {
                s.gameOver = true;
                s.victory = (f.id == s.playerId);
                s.gameOverReason = "House " + f.surname + " amassed one million florins!";
                return;
            }
        }

        // 3. If only the player remains (all rivals eliminated), they win outright.
        int survivors = 0;
        Family lastStanding = null;
        for (Family f : s.families) {
            if (!f.eliminated) { survivors++; lastStanding = f; }
        }
        if (survivors == 1 && lastStanding != null) {
            s.gameOver = true;
            s.victory = (lastStanding.id == s.playerId);
            s.gameOverReason = "House " + lastStanding.surname
                    + " stands alone; all rivals have fallen.";
            return;
        }

        // 4. End-of-era: highest net worth wins.
        if (s.year >= s.endYear) {
            List<Score> ranked = ranking(s);
            Score top = ranked.isEmpty() ? null : ranked.get(0);
            s.gameOver = true;
            s.victory = (top != null && top.family.id == s.playerId);
            s.gameOverReason = (top == null) ? "" : "House " + top.family.surname
                    + " ends the era wealthiest (" + top.netWorth + " florins).";
        }
    }
}
