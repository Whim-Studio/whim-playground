package com.tiwas.mahjong.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.tiwas.mahjong.model.Dragon;
import com.tiwas.mahjong.model.Hand;
import com.tiwas.mahjong.model.Meld;
import com.tiwas.mahjong.model.MeldType;
import com.tiwas.mahjong.model.Tile;
import com.tiwas.mahjong.model.TileSuit;
import com.tiwas.mahjong.model.Wind;

/**
 * Pure analysis of tile collections: standard-win decomposition (4 sets + a
 * pair) and the two special limit hands. No game or scoring state lives here.
 */
public final class HandAnalyzer {

    /** One complete parse of a hand into its melds and pair. */
    public static final class Decomposition {
        public final List<Meld> melds;   // includes the already-fixed melds
        public final List<Tile> pair;

        public Decomposition(List<Meld> melds, List<Tile> pair) {
            this.melds = melds;
            this.pair = pair;
        }
    }

    private static final Comparator<Tile> ORDER = new Comparator<Tile>() {
        public int compare(Tile a, Tile b) {
            return sortKey(a) - sortKey(b);
        }
    };

    private static int sortKey(Tile t) {
        int suit = t.getSuit().ordinal();
        int sec;
        if (t.isSuited()) {
            sec = t.getRank();
        } else if (t.getSuit() == TileSuit.WIND) {
            sec = t.getWind().ordinal();
        } else if (t.getSuit() == TileSuit.DRAGON) {
            sec = t.getDragon().ordinal();
        } else {
            sec = 0;
        }
        return suit * 100 + sec;
    }

    private HandAnalyzer() {
    }

    /**
     * All ways to split the given concealed tiles into {@code setsNeeded} sets
     * plus exactly one pair. Each returned decomposition lists only the melds
     * formed from these concealed tiles (the caller adds the fixed melds).
     */
    public static List<Decomposition> decomposeConcealed(List<Tile> concealed, int setsNeeded) {
        List<Decomposition> out = new ArrayList<Decomposition>();
        if (concealed.size() != setsNeeded * 3 + 2) {
            return out;
        }
        List<Tile> sorted = new ArrayList<Tile>(concealed);
        Collections.sort(sorted, ORDER);

        // Choose the pair first, then split the rest into sets.
        int i = 0;
        while (i < sorted.size()) {
            Tile t = sorted.get(i);
            int j = i;
            while (j < sorted.size() && sorted.get(j).equals(t)) {
                j++;
            }
            int count = j - i;
            if (count >= 2) {
                List<Tile> rest = new ArrayList<Tile>(sorted);
                // remove two copies of t for the pair
                rest.remove(t);
                rest.remove(t);
                List<List<Meld>> setSplits = new ArrayList<List<Meld>>();
                splitIntoSets(rest, new ArrayList<Meld>(), setSplits);
                for (int k = 0; k < setSplits.size(); k++) {
                    List<Tile> pair = new ArrayList<Tile>();
                    pair.add(t);
                    pair.add(t);
                    out.add(new Decomposition(setSplits.get(k), pair));
                }
            }
            i = j;
        }
        return out;
    }

    /** Recursively split a sorted-able tile list entirely into pungs and chows. */
    private static void splitIntoSets(List<Tile> tiles, List<Meld> acc, List<List<Meld>> out) {
        if (tiles.isEmpty()) {
            out.add(new ArrayList<Meld>(acc));
            return;
        }
        List<Tile> sorted = new ArrayList<Tile>(tiles);
        Collections.sort(sorted, ORDER);
        Tile first = sorted.get(0);

        // Option 1: pung of the first tile.
        if (countOf(sorted, first) >= 3) {
            List<Tile> rest = new ArrayList<Tile>(sorted);
            rest.remove(first);
            rest.remove(first);
            rest.remove(first);
            List<Tile> meldTiles = new ArrayList<Tile>();
            meldTiles.add(first);
            meldTiles.add(first);
            meldTiles.add(first);
            acc.add(new Meld(MeldType.PUNG, meldTiles, true));
            splitIntoSets(rest, acc, out);
            acc.remove(acc.size() - 1);
        }

        // Option 2: chow starting at the first tile (suited only).
        if (first.isSuited() && first.getRank() <= 7) {
            Tile second = Tile.suited(first.getSuit(), first.getRank() + 1);
            Tile third = Tile.suited(first.getSuit(), first.getRank() + 2);
            if (sorted.contains(second) && sorted.contains(third)) {
                List<Tile> rest = new ArrayList<Tile>(sorted);
                rest.remove(first);
                rest.remove(second);
                rest.remove(third);
                List<Tile> meldTiles = new ArrayList<Tile>();
                meldTiles.add(first);
                meldTiles.add(second);
                meldTiles.add(third);
                acc.add(new Meld(MeldType.CHOW, meldTiles, true));
                splitIntoSets(rest, acc, out);
                acc.remove(acc.size() - 1);
            }
        }
    }

    private static int countOf(List<Tile> tiles, Tile t) {
        int n = 0;
        for (int i = 0; i < tiles.size(); i++) {
            if (tiles.get(i).equals(t)) {
                n++;
            }
        }
        return n;
    }

    /**
     * Is the hand (existing melds + the given concealed tiles) a complete
     * standard win? Existing kong melds each count as one set.
     */
    public static boolean isStandardWin(List<Meld> fixedMelds, List<Tile> concealed) {
        int setsNeeded = 4 - fixedMelds.size();
        if (setsNeeded < 0) {
            return false;
        }
        return !decomposeConcealed(concealed, setsNeeded).isEmpty();
    }

    /** Convenience: would these concealed tiles plus the candidate win the hand? */
    public static boolean wouldWin(Hand hand, List<Tile> concealedWithWinTile) {
        return isStandardWin(hand.getMelds(), concealedWithWinTile)
                || isThirteenOrphans(concealedWithWinTile, hand.getMelds());
    }

    /**
     * Thirteen Orphans: one of each terminal-or-honour (13 kinds) plus one
     * duplicate, fully concealed (no exposed melds). 14 tiles total.
     */
    public static boolean isThirteenOrphans(List<Tile> concealed, List<Meld> melds) {
        if (!melds.isEmpty()) {
            return false;
        }
        if (concealed.size() != 14) {
            return false;
        }
        List<Tile> needed = thirteenOrphanFaces();
        boolean sawPair = false;
        for (int n = 0; n < needed.size(); n++) {
            int c = countOf(concealed, needed.get(n));
            if (c == 0) {
                return false;
            }
            if (c == 2) {
                if (sawPair) {
                    return false;
                }
                sawPair = true;
            } else if (c > 2) {
                return false;
            }
        }
        return sawPair;
    }

    /** The 13 distinct terminal-and-honour faces. */
    public static List<Tile> thirteenOrphanFaces() {
        List<Tile> faces = new ArrayList<Tile>();
        TileSuit[] suits = { TileSuit.DOTS, TileSuit.BAMBOO, TileSuit.CHARACTERS };
        for (int s = 0; s < suits.length; s++) {
            faces.add(Tile.suited(suits[s], 1));
            faces.add(Tile.suited(suits[s], 9));
        }
        Wind[] winds = Wind.values();
        for (int w = 0; w < winds.length; w++) {
            faces.add(Tile.wind(winds[w]));
        }
        Dragon[] dragons = Dragon.values();
        for (int d = 0; d < dragons.length; d++) {
            faces.add(Tile.dragon(dragons[d]));
        }
        return faces;
    }

    /** Sort a tile list in display order (in place copy returned). */
    public static List<Tile> sorted(List<Tile> tiles) {
        List<Tile> copy = new ArrayList<Tile>(tiles);
        Collections.sort(copy, ORDER);
        return copy;
    }
}
