package com.tiwa.mahjong.engine;

import com.tiwa.mahjong.api.Meld;
import com.tiwa.mahjong.api.MeldType;
import com.tiwa.mahjong.api.Suit;
import com.tiwa.mahjong.api.Tile;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Validates the structure of a winning hand (Section 5): exactly 4 sets + 1 pair, where a set is a
 * pung, kong, or chow. Declared chows must be concealed.
 *
 * <p>This class decides LEGALITY only; scoring is Task 3's responsibility.</p>
 *
 * <p>Inputs: the player's still-concealed tiles, their declared melds (already-formed sets, plus
 * possibly a declared pair), and the winning tile (the discard claimed or the tile self-drawn).
 * The concealed tiles together with the winning tile must decompose into the sets and pair not
 * already declared. Kongs count as a single set; their physical 4th tile lives in the declared meld,
 * not in the concealed-tile count.</p>
 */
public final class MahjongValidator {

    private static final int REQUIRED_SETS = 4;
    private static final int REQUIRED_PAIRS = 1;

    /**
     * @param concealedTiles tiles still hidden in hand (excluding the winning tile)
     * @param declaredMelds  melds already declared (pung/kong/chow sets and any declared pair)
     * @param winningTile    the tile that completes the hand (added to the concealed tiles)
     * @return true if this is a legal 4-sets + 1-pair winning hand
     */
    public boolean isWinningHand(List<Tile> concealedTiles, List<Meld> declaredMelds, Tile winningTile) {
        if (concealedTiles == null || declaredMelds == null || winningTile == null) {
            return false;
        }

        int declaredSets = 0;
        int declaredPairs = 0;
        for (Meld meld : declaredMelds) {
            MeldType type = meld.getType();
            if (type == MeldType.CHOW) {
                if (!meld.isConcealed()) {
                    return false; // chows must be concealed
                }
                declaredSets++;
            } else if (type == MeldType.PUNG || type == MeldType.KONG) {
                declaredSets++;
            } else if (type == MeldType.PAIR) {
                declaredPairs++;
            } else {
                return false;
            }
        }

        int needSets = REQUIRED_SETS - declaredSets;
        int needPairs = REQUIRED_PAIRS - declaredPairs;
        if (needSets < 0 || needPairs < 0) {
            return false;
        }

        List<Tile> working = new ArrayList<Tile>(concealedTiles);
        working.add(winningTile);

        // Bonus tiles (flowers/seasons) never form part of a hand.
        for (Tile t : working) {
            if (t.isBonus()) {
                return false;
            }
        }

        if (working.size() != needSets * 3 + needPairs * 2) {
            return false;
        }

        TreeMap<Integer, Integer> counts = new TreeMap<Integer, Integer>();
        for (Tile t : working) {
            int key = encode(t);
            Integer c = counts.get(key);
            counts.put(key, c == null ? 1 : c + 1);
        }

        return decompose(counts, needSets, needPairs);
    }

    /** Encode a (suit, rank) into a single sortable key; consecutive ranks of one suit are adjacent. */
    private static int encode(Tile t) {
        return t.getSuit().ordinal() * 100 + t.getRank();
    }

    private static boolean isSuited(int suitOrdinal) {
        return Suit.values()[suitOrdinal].isSuited();
    }

    /**
     * Recursively decide whether the remaining tiles form exactly {@code needSets} sets and
     * {@code needPairs} pairs. Always works from the smallest remaining key, which must begin some
     * group, trying pair / pung / chow in turn.
     */
    private static boolean decompose(TreeMap<Integer, Integer> counts, int needSets, int needPairs) {
        int firstKey = -1;
        for (java.util.Map.Entry<Integer, Integer> e : counts.entrySet()) {
            if (e.getValue() > 0) {
                firstKey = e.getKey();
                break;
            }
        }
        if (firstKey == -1) {
            return needSets == 0 && needPairs == 0;
        }

        int suit = firstKey / 100;
        int rank = firstKey % 100;
        int available = counts.get(firstKey);

        // Try a pair starting here.
        if (needPairs > 0 && available >= 2) {
            adjust(counts, firstKey, -2);
            if (decompose(counts, needSets, needPairs - 1)) {
                adjust(counts, firstKey, 2);
                return true;
            }
            adjust(counts, firstKey, 2);
        }

        // Try a pung starting here.
        if (needSets > 0 && available >= 3) {
            adjust(counts, firstKey, -3);
            if (decompose(counts, needSets - 1, needPairs)) {
                adjust(counts, firstKey, 3);
                return true;
            }
            adjust(counts, firstKey, 3);
        }

        // Try a chow starting here (suited only, and within rank 1-9 of the same suit).
        if (needSets > 0 && isSuited(suit) && rank <= 7) {
            int k2 = firstKey + 1;
            int k3 = firstKey + 2;
            if (count(counts, k2) >= 1 && count(counts, k3) >= 1) {
                adjust(counts, firstKey, -1);
                adjust(counts, k2, -1);
                adjust(counts, k3, -1);
                if (decompose(counts, needSets - 1, needPairs)) {
                    adjust(counts, firstKey, 1);
                    adjust(counts, k2, 1);
                    adjust(counts, k3, 1);
                    return true;
                }
                adjust(counts, firstKey, 1);
                adjust(counts, k2, 1);
                adjust(counts, k3, 1);
            }
        }

        return false;
    }

    private static int count(TreeMap<Integer, Integer> counts, int key) {
        Integer c = counts.get(key);
        return c == null ? 0 : c;
    }

    private static void adjust(TreeMap<Integer, Integer> counts, int key, int delta) {
        counts.put(key, count(counts, key) + delta);
    }
}
