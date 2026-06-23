package com.tiwas.mahjong.engine;

import java.util.ArrayList;
import java.util.List;

import com.tiwas.mahjong.model.GameState;
import com.tiwas.mahjong.model.Hand;
import com.tiwas.mahjong.model.Meld;
import com.tiwas.mahjong.model.MeldType;
import com.tiwas.mahjong.model.Tile;

/**
 * Works out which players may claim a discard and resolves competing claims.
 *
 * Per the rules, a discard may be claimed only for a Pung or a Mahjong (Kong
 * from a discard is not allowed, and Chows are never formed from a discard).
 * Priority is Mahjong &gt; Pung; among multiple mahjong claims the one nearest
 * counter-clockwise to the discarder wins.
 */
public final class ClaimResolver {

    /** A possible claim by one player. */
    public static final class Claim {
        public final int playerIndex;
        public final MeldType type;   // PUNG, or null for a mahjong claim
        public final boolean mahjong;

        public Claim(int playerIndex, MeldType type, boolean mahjong) {
            this.playerIndex = playerIndex;
            this.type = type;
            this.mahjong = mahjong;
        }
    }

    private ClaimResolver() {
    }

    /** Can this player pung the discard? */
    public static boolean canPung(Hand hand, Tile discard) {
        return hand.count(discard) >= 2;
    }

    /** Can this player win on the discard (pung/pair completion or 13 orphans)? */
    public static boolean canMahjong(Hand hand, Tile discard) {
        List<Tile> trial = new ArrayList<Tile>(hand.getTiles());
        trial.add(discard);

        // Thirteen Orphans completion (fully concealed).
        if (HandAnalyzer.isThirteenOrphans(trial, hand.getMelds())) {
            return true;
        }
        // Standard win in which the discard sits in a pung or the pair
        // (a chow may not be completed from a discard).
        int setsNeeded = 4 - hand.getMelds().size();
        List<HandAnalyzer.Decomposition> parses =
                HandAnalyzer.decomposeConcealed(trial, setsNeeded);
        for (int i = 0; i < parses.size(); i++) {
            if (winTileInPungOrPair(parses.get(i), discard)) {
                return true;
            }
        }
        return false;
    }

    private static boolean winTileInPungOrPair(HandAnalyzer.Decomposition d, Tile winTile) {
        if (d.pair.size() == 2 && d.pair.get(0).equals(winTile)) {
            return true;
        }
        for (int i = 0; i < d.melds.size(); i++) {
            Meld m = d.melds.get(i);
            if (m.isPung() && m.representative().equals(winTile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolve a set of candidate claims into the single winning claim, applying
     * Mahjong &gt; Pung priority and the counter-clockwise tie-break for
     * multiple mahjong claims. Returns null if there are no claims.
     */
    public static Claim resolve(GameState state, int discarder, List<Claim> claims) {
        if (claims.isEmpty()) {
            return null;
        }
        // Mahjong first.
        List<Claim> mahjongs = new ArrayList<Claim>();
        Claim pung = null;
        for (int i = 0; i < claims.size(); i++) {
            Claim c = claims.get(i);
            if (c.mahjong) {
                mahjongs.add(c);
            } else if (c.type == MeldType.PUNG && pung == null) {
                pung = c;
            }
        }
        if (!mahjongs.isEmpty()) {
            // nearest counter-clockwise to the discarder wins
            int seat = state.nextSeat(discarder);
            for (int step = 0; step < 4; step++) {
                for (int i = 0; i < mahjongs.size(); i++) {
                    if (mahjongs.get(i).playerIndex == seat) {
                        return mahjongs.get(i);
                    }
                }
                seat = state.nextSeat(seat);
            }
            return mahjongs.get(0);
        }
        return pung;
    }
}
