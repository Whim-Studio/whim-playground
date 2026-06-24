package com.dglz.ai;

import com.dglz.domain.Card;
import com.dglz.domain.Combination;
import com.dglz.domain.ComboType;
import com.dglz.domain.PlayerStrategy;
import com.dglz.domain.Player;
import com.dglz.domain.Rank;
import com.dglz.domain.Road;
import com.dglz.engine.ComboValidator;
import com.dglz.engine.GameState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI opponent strategy for Da Guai Lu Zi.
 *
 * <p>Core principles, in priority order:</p>
 * <ol>
 *   <li><b>Team synergy</b> &mdash; if a teammate is already winning the trick and we are
 *       not forced to lead, PASS. Never overtake your own partner.</li>
 *   <li><b>Lead cheaply</b> &mdash; when leading, shed weak/awkward cards and keep bombs and
 *       jokers in reserve.</li>
 *   <li><b>Follow cheaply</b> &mdash; when an opponent is winning, take the trick with the
 *       smallest combination that beats it; don't break up pairs/triples/bombs or spend a
 *       joker to win a trivial single.</li>
 * </ol>
 *
 * <p>No-arg constructible so {@code app.Main} can {@code new} it directly.</p>
 */
public class AiStrategy implements PlayerStrategy {

    public AiStrategy() {
        // no-arg
    }

    @Override
    public Combination decideMove(GameState state, int seat) {
        Player me = state.players().get(seat);
        List<Card> hand = me.hand();
        if (hand == null || hand.isEmpty()) {
            return null;
        }
        Combination toBeat = state.currentBest();
        boolean leading = (toBeat == null);

        // 1. Team synergy: don't outbid a winning teammate unless we must lead.
        if (!leading && HandEvaluator.teammateWinning(state, seat)) {
            return null;
        }

        if (leading) {
            return chooseLead(hand);
        }
        return chooseFollow(hand, state.currentRoad(), toBeat);
    }

    // ------------------------------------------------------------------
    // Shared decision used by both the AI and the Coach.
    // ------------------------------------------------------------------

    /**
     * Computes the move this strategy would make for {@code seat}. Exposed so the Coach
     * can reuse the exact same reasoning to advise the human. Returns {@code null} for PASS.
     */
    public Combination chooseBest(GameState state, int seat) {
        return decideMove(state, seat);
    }

    // ------------------------------------------------------------------
    // Leading: shed weak cards, keep bombs and jokers.
    // ------------------------------------------------------------------

    private Combination chooseLead(List<Card> hand) {
        List<Combination> candidates = new ArrayList<Combination>();
        addAll(candidates, ComboValidator.enumerate(hand, Road.SINGLE, null));
        addAll(candidates, ComboValidator.enumerate(hand, Road.PAIR, null));
        addAll(candidates, ComboValidator.enumerate(hand, Road.TRIPLE, null));
        addAll(candidates, ComboValidator.enumerate(hand, Road.FIVE, null));

        Combination best = null;
        double bestScore = Double.MAX_VALUE;
        for (Combination c : candidates) {
            double s = leadScore(c, hand);
            if (s < bestScore) {
                bestScore = s;
                best = c;
            }
        }
        if (best != null) {
            return best;
        }
        // Defensive fallback: lead the single lowest card (a player who must lead
        // can never legally pass). Should be unreachable with a non-empty hand.
        return ComboValidator.identify(Collections.singletonList(lowestCard(hand)));
    }

    /** Lower is a better lead. */
    private double leadScore(Combination c, List<Card> hand) {
        double s = 0.0;
        if (isStrongFive(c.type())) {
            s += 1000.0; // never volunteer a bomb when leading
        }
        s += c.wildcardsUsed() * 200.0;     // keep jokers in reserve
        s += c.primaryRank().order() * 3.0; // shed low ranks first
        s -= c.size() * 2.0;                // shedding more cards at once is progress
        s += breakPenalty(c, hand);         // don't fracture a natural group to lead
        return s;
    }

    // ------------------------------------------------------------------
    // Following: win as cheaply as possible, hold resources for real fights.
    // ------------------------------------------------------------------

    private Combination chooseFollow(List<Card> hand, Road road, Combination toBeat) {
        List<Combination> options = ComboValidator.enumerate(hand, road, toBeat);
        if (options.isEmpty()) {
            return null; // nothing legal beats the current best
        }
        Combination best = null;
        double bestScore = Double.MAX_VALUE;
        for (Combination c : options) {
            double s = followScore(c, hand);
            if (s < bestScore) {
                bestScore = s;
                best = c;
            }
        }
        if (best == null) {
            return null;
        }

        // Hold strong resources for contested tricks: it's not worth breaking a
        // natural group or burning a joker/bomb just to win a trivial low single.
        boolean trivialSingle = road == Road.SINGLE
                && toBeat.primaryRank().order() < Rank.KING.order();
        boolean wasteful = best.wildcardsUsed() > 0
                || isStrongFive(best.type())
                || breakPenalty(best, hand) > 0;
        if (trivialSingle && wasteful) {
            return null; // pass and keep our powder dry
        }
        return best;
    }

    /** Lower is a cheaper, more economical win. */
    private double followScore(Combination c, List<Card> hand) {
        double s = 0.0;
        s += c.type().tier() * 100.0;       // cheapest combo type first (e.g. plain straight)
        s += c.primaryRank().order() * 2.0; // then the lowest rank that still wins
        s += c.wildcardsUsed() * 80.0;      // avoid spending jokers
        if (isStrongFive(c.type())) {
            s += 500.0;                     // hold bombs unless they are the only option
        }
        s += breakPenalty(c, hand);         // avoid fracturing a natural group
        return s;
    }

    // ------------------------------------------------------------------
    // Helpers.
    // ------------------------------------------------------------------

    /**
     * Penalty for consuming part of a larger natural group than the combo itself uses
     * (e.g. spending one card of a pair as a single, or three of a four-bomb as a triple).
     */
    private double breakPenalty(Combination c, List<Card> hand) {
        // Only meaningful for the 1/2/3 roads; five-card combos intentionally use groups.
        ComboType t = c.type();
        if (t == ComboType.SINGLE) {
            int n = handCount(hand, c.primaryRank());
            if (n >= 4) return 600.0;
            if (n == 3) return 150.0;
            if (n == 2) return 40.0;
            return 0.0;
        }
        if (t == ComboType.PAIR) {
            int n = handCount(hand, c.primaryRank());
            if (n >= 4) return 500.0;
            if (n == 3) return 60.0;
            return 0.0;
        }
        if (t == ComboType.TRIPLE) {
            int n = handCount(hand, c.primaryRank());
            if (n >= 4) return 400.0;
            return 0.0;
        }
        return 0.0;
    }

    static boolean isStrongFive(ComboType t) {
        return t == ComboType.FOUR_PLUS_ONE
                || t == ComboType.STRAIGHT_FLUSH
                || t == ComboType.FIVE_OF_A_KIND;
    }

    private int handCount(List<Card> hand, Rank rank) {
        int n = 0;
        for (Card c : hand) {
            if (c.rank() == rank) {
                n++;
            }
        }
        return n;
    }

    private Card lowestCard(List<Card> hand) {
        Card low = hand.get(0);
        for (Card c : hand) {
            if (c.rank().order() < low.rank().order()) {
                low = c;
            }
        }
        return low;
    }

    private void addAll(List<Combination> target, List<Combination> src) {
        if (src != null) {
            target.addAll(src);
        }
    }
}
