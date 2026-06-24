package com.dglz.engine;

import com.dglz.domain.Card;
import com.dglz.domain.Combination;
import com.dglz.domain.ComboType;
import com.dglz.domain.Rank;
import com.dglz.domain.Road;
import com.dglz.domain.Suit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure rules core: combination identification (including joker wildcards completing
 * 5-card hands), follow legality, and move enumeration. No state.
 */
public final class ComboValidator {

    private static final Suit[] RANKED_SUITS = { Suit.CLUBS, Suit.DIAMONDS, Suit.HEARTS, Suit.SPADES };

    /**
     * Return the single best legal Combination these cards form (applying jokers as
     * wildcards to complete 5-card hands), or null if they form no legal combination.
     */
    public Combination identify(List<Card> cards) {
        if (cards == null) {
            return null;
        }
        int n = cards.size();
        Road road = Road.forSize(n);
        if (road == null) {
            return null;
        }
        switch (road) {
            case SINGLE:
                return identifySingle(cards);
            case PAIR:
                return identifyNOfAKind(cards, Road.PAIR, ComboType.PAIR);
            case TRIPLE:
                return identifyNOfAKind(cards, Road.TRIPLE, ComboType.TRIPLE);
            case FIVE:
                return identifyFive(cards);
            default:
                return null;
        }
    }

    private Combination identifySingle(List<Card> cards) {
        Card c = cards.get(0);
        // A bare joker is its own rank; never a wildcard fill in a single.
        return new Combination(Road.SINGLE, ComboType.SINGLE, copy(cards), c.rank(), 0);
    }

    private Combination identifyNOfAKind(List<Card> cards, Road road, ComboType type) {
        Rank best = bestSameRank(cards);
        if (best == null) {
            return null;
        }
        int wild = serveAs(cards, best);
        return new Combination(road, type, copy(cards), best, wild);
    }

    private Combination identifyFive(List<Card> cards) {
        int jokers = countJokers(cards);

        // 1. FIVE_OF_A_KIND
        Rank quint = bestSameRank(cards);
        if (quint != null) {
            return new Combination(Road.FIVE, ComboType.FIVE_OF_A_KIND, copy(cards), quint,
                serveAs(cards, quint));
        }

        // 2. STRAIGHT_FLUSH
        Combination sf = tryStraight(cards, jokers, true);
        if (sf != null) {
            return sf;
        }

        // 3. FOUR_PLUS_ONE
        Combination fpo = tryFourPlusOne(cards);
        if (fpo != null) {
            return fpo;
        }

        // 4. FULL_HOUSE
        Combination fh = tryFullHouse(cards, jokers);
        if (fh != null) {
            return fh;
        }

        // 5. FLUSH
        Combination fl = tryFlush(cards, jokers);
        if (fl != null) {
            return fl;
        }

        // 6. STRAIGHT
        return tryStraight(cards, jokers, false);
    }

    // ---- helpers ----

    /** Number of wildcards (jokers) needed for all cards to serve as rank R, or -1 if impossible. */
    private int serveAs(List<Card> cards, Rank r) {
        int used = 0;
        for (int i = 0; i < cards.size(); i++) {
            Card c = cards.get(i);
            if (c.rank() == r) {
                continue; // natural match (includes a joker matching its own joker rank)
            }
            if (c.isWildcard()) {
                if (!r.isJoker()) {
                    used++; // any joker fills a natural rank
                } else if (r == Rank.SMALL_JOKER && c.isBigJoker()) {
                    used++; // big joker may stand in for a small joker
                } else {
                    return -1; // small joker may NOT represent a big joker
                }
            } else {
                return -1; // a natural card cannot become a different rank
            }
        }
        return used;
    }

    /** Highest-order rank that all cards can serve as, or null. */
    private Rank bestSameRank(List<Card> cards) {
        Rank best = null;
        for (Rank r : Rank.values()) {
            if (serveAs(cards, r) >= 0) {
                if (best == null || r.order() > best.order()) {
                    best = r;
                }
            }
        }
        return best;
    }

    private int countJokers(List<Card> cards) {
        int j = 0;
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).isWildcard()) {
                j++;
            }
        }
        return j;
    }

    /** Straight or straight-flush over the 5 cards. requireFlush controls the suit constraint. */
    private Combination tryStraight(List<Card> cards, int jokers, boolean requireFlush) {
        Rank[] nat = Rank.naturalAscending();
        Combination best = null;

        if (requireFlush) {
            for (Suit suit : RANKED_SUITS) {
                Combination c = straightForSuit(cards, nat, suit, jokers);
                if (c != null && (best == null || c.primaryRank().order() > best.primaryRank().order())) {
                    best = c;
                }
            }
            return best;
        }
        return straightForSuit(cards, nat, null, jokers);
    }

    /** suit == null means mixed-suit straight; otherwise all naturals must be that suit. */
    private Combination straightForSuit(List<Card> cards, Rank[] nat, Suit suit, int jokers) {
        Set<Rank> naturalRanks = new HashSet<Rank>();
        for (int i = 0; i < cards.size(); i++) {
            Card c = cards.get(i);
            if (c.isWildcard()) {
                continue;
            }
            if (suit != null && c.suit() != suit) {
                return null; // wrong suit for a flush
            }
            if (!naturalRanks.add(c.rank())) {
                return null; // duplicate rank cannot fit a straight
            }
        }
        Combination best = null;
        for (int start = 0; start + 5 <= nat.length; start++) {
            Set<Rank> window = new HashSet<Rank>();
            for (int k = 0; k < 5; k++) {
                window.add(nat[start + k]);
            }
            boolean allInWindow = true;
            for (Rank r : naturalRanks) {
                if (!window.contains(r)) {
                    allInWindow = false;
                    break;
                }
            }
            if (!allInWindow) {
                continue;
            }
            // jokers fill the (5 - naturalRanks.size()) missing positions, which equals `jokers`.
            Rank primary = nat[start + 4];
            ComboType type = (suit != null) ? ComboType.STRAIGHT_FLUSH : ComboType.STRAIGHT;
            Combination cand = new Combination(Road.FIVE, type, copy(cards), primary, jokers);
            if (best == null || primary.order() > best.primaryRank().order()) {
                best = cand;
            }
        }
        return best;
    }

    private Combination tryFourPlusOne(List<Card> cards) {
        Combination best = null;
        for (int leftover = 0; leftover < cards.size(); leftover++) {
            List<Card> quad = new ArrayList<Card>(4);
            for (int i = 0; i < cards.size(); i++) {
                if (i != leftover) {
                    quad.add(cards.get(i));
                }
            }
            Rank r = bestSameRank(quad);
            if (r != null) {
                int wild = serveAs(quad, r);
                if (best == null || r.order() > best.primaryRank().order()) {
                    best = new Combination(Road.FIVE, ComboType.FOUR_PLUS_ONE, copy(cards), r, wild);
                }
            }
        }
        return best;
    }

    private Combination tryFullHouse(List<Card> cards, int jokers) {
        Rank[] nat = Rank.naturalAscending();
        // count naturals per rank
        int[] count = new int[Rank.values().length];
        Set<Rank> presentNatural = new HashSet<Rank>();
        for (int i = 0; i < cards.size(); i++) {
            Card c = cards.get(i);
            if (!c.isWildcard()) {
                count[c.rank().ordinal()]++;
                presentNatural.add(c.rank());
            }
        }
        Rank bestA = null;
        for (Rank a : nat) {
            for (Rank b : nat) {
                if (a == b) {
                    continue;
                }
                boolean ok = true;
                for (Rank r : presentNatural) {
                    if (r != a && r != b) {
                        ok = false;
                        break;
                    }
                }
                if (!ok) {
                    continue;
                }
                int ca = count[a.ordinal()];
                int cb = count[b.ordinal()];
                if (ca <= 3 && cb <= 2 && (3 - ca) + (2 - cb) == jokers) {
                    if (bestA == null || a.order() > bestA.order()) {
                        bestA = a;
                    }
                }
            }
        }
        if (bestA == null) {
            return null;
        }
        return new Combination(Road.FIVE, ComboType.FULL_HOUSE, copy(cards), bestA, jokers);
    }

    private Combination tryFlush(List<Card> cards, int jokers) {
        for (Suit suit : RANKED_SUITS) {
            boolean ok = true;
            Rank maxNatural = null;
            for (int i = 0; i < cards.size(); i++) {
                Card c = cards.get(i);
                if (c.isWildcard()) {
                    continue;
                }
                if (c.suit() != suit) {
                    ok = false;
                    break;
                }
                if (maxNatural == null || c.rank().order() > maxNatural.order()) {
                    maxNatural = c.rank();
                }
            }
            if (!ok) {
                continue;
            }
            // This suit holds all naturals; jokers assume the suit. (5 naturals of one suit
            // that are also consecutive would already be a straight flush, caught earlier.)
            Rank primary = maxNatural;
            if (jokers > 0) {
                if (primary == null || Rank.TWO.order() > primary.order()) {
                    primary = Rank.TWO; // a joker can act as the highest flush card
                }
            }
            if (primary == null) {
                continue;
            }
            return new Combination(Road.FIVE, ComboType.FLUSH, copy(cards), primary, jokers);
        }
        return null;
    }

    public boolean sameRoad(Combination a, Combination b) {
        return a.road() == b.road();
    }

    /** candidate must be the same Road as lead and beat it. */
    public boolean isLegalFollow(Combination lead, Combination candidate) {
        if (lead == null || candidate == null) {
            return false;
        }
        if (candidate.road() != lead.road()) {
            return false;
        }
        return candidate.beats(lead);
    }

    /**
     * All legal combinations from hand for the given leadRoad that beat toBeat
     * (toBeat null when leading). Deduplicated by combo type + card faces.
     * The FIVE road is enumerated exhaustively over all C(n,5) card subsets of the hand
     * (n <= 27), which is bounded and fast for realistic hand sizes (see README).
     */
    public List<Combination> enumerate(List<Card> hand, Road leadRoad, Combination toBeat) {
        List<Combination> out = new ArrayList<Combination>();
        if (hand == null || leadRoad == null) {
            return out;
        }
        int k = leadRoad.size();
        if (k > hand.size()) {
            return out;
        }
        Set<String> seen = new LinkedHashSet<String>();
        combine(hand, k, 0, new ArrayList<Card>(k), out, seen, leadRoad, toBeat);
        return out;
    }

    private void combine(List<Card> hand, int k, int start, List<Card> acc,
                         List<Combination> out, Set<String> seen, Road leadRoad, Combination toBeat) {
        if (acc.size() == k) {
            Combination combo = identify(acc);
            if (combo == null || combo.road() != leadRoad) {
                return;
            }
            if (toBeat != null && !combo.beats(toBeat)) {
                return;
            }
            String key = comboKey(combo);
            if (seen.add(key)) {
                out.add(combo);
            }
            return;
        }
        int remainingNeeded = k - acc.size();
        for (int i = start; i <= hand.size() - remainingNeeded; i++) {
            acc.add(hand.get(i));
            combine(hand, k, i + 1, acc, out, seen, leadRoad, toBeat);
            acc.remove(acc.size() - 1);
        }
    }

    private String comboKey(Combination combo) {
        List<String> faces = new ArrayList<String>();
        for (Card c : combo.cards()) {
            faces.add(c.shortName());
        }
        Collections.sort(faces);
        return combo.type().name() + faces.toString();
    }

    private static List<Card> copy(List<Card> cards) {
        return new ArrayList<Card>(cards);
    }
}
