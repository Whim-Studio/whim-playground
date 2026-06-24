package com.dglz.ai;

import com.dglz.domain.Card;
import com.dglz.domain.Team;
import com.dglz.engine.GameState;

import java.util.List;

/**
 * Static evaluation helpers shared by {@link AiStrategy} and {@link CoachTranslator}.
 *
 * <p>The heuristics here are intentionally simple and deterministic: more high cards,
 * bombs and jokers make a hand stronger, while a hand full of scattered singles is
 * harder to shed and therefore weaker.</p>
 */
public final class HandEvaluator {

    private HandEvaluator() {
        // static-only utility
    }

    /**
     * Heuristic strength score for a hand. Higher is stronger.
     *
     * <p>Scoring intuition:</p>
     * <ul>
     *   <li>Every card is worth roughly its rank order (so Aces/2s/jokers count more).</li>
     *   <li>Jokers carry a large bonus &mdash; they are the most flexible cards in the game.</li>
     *   <li>Natural groups are rewarded: pairs &lt; triples &lt; four-of-a-kind &lt; five-of-a-kind,
     *       because a group of four or five is a bomb.</li>
     *   <li>Lone singles get a small penalty &mdash; a hand of awkward, unconnected cards is weak.</li>
     * </ul>
     */
    public static int handStrength(List<Card> hand) {
        if (hand == null || hand.isEmpty()) {
            return 0;
        }
        // Rank order ranges 3..17 (jokers are 16/17). Index by order for counting.
        int[] counts = new int[18];
        int score = 0;
        for (Card c : hand) {
            int order = c.rank().order();
            counts[order]++;
            // Base value: a card is worth (order - 2), i.e. a "3" is 1, an Ace is 12.
            score += Math.max(0, order - 2);
            if (c.isWildcard()) {
                score += 12; // jokers are wildcards and extremely valuable
            } else if (order >= 14) {
                score += 4;  // Aces and 2s (the high natural cards) get a bonus
            }
        }
        for (int order = 3; order <= 17; order++) {
            int n = counts[order];
            if (n >= 5) {
                score += 40; // five-of-a-kind: top bomb
            } else if (n == 4) {
                score += 25; // four-of-a-kind: bomb material
            } else if (n == 3) {
                score += 8;  // triple
            } else if (n == 2) {
                score += 3;  // pair
            } else if (n == 1) {
                score -= 1;  // scattered single, mildly awkward
            }
        }
        return score;
    }

    /**
     * True iff the seat currently winning the trick ({@code currentBestSeat}) is a
     * different seat on the SAME team as {@code seat}. Used to enforce the team-synergy
     * rule: never outbid your own teammate.
     */
    public static boolean teammateWinning(GameState state, int seat) {
        int bestSeat = state.currentBestSeat();
        if (bestSeat < 0 || bestSeat == seat) {
            return false;
        }
        return Team.forSeat(bestSeat) == Team.forSeat(seat);
    }
}
