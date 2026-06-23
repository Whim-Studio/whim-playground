package com.tiwas.mahjong.engine;

import java.util.List;
import java.util.Random;

import com.tiwas.mahjong.model.Hand;
import com.tiwas.mahjong.model.Tile;
import com.tiwas.mahjong.model.TileSuit;
import com.tiwas.mahjong.model.Wind;

/**
 * Simple, legal heuristics for the three computer seats.
 *
 * Discarding: keep pairs, triplets and near-runs; throw isolated honours and
 * terminals first. Claiming: always take a winning mahjong; pung when the tile
 * is valuable (an honour or a tile that gives a second set) so the AI does not
 * fritter away a concealed hand on every loose pair.
 */
public final class AIPlayerLogic {

    public enum ClaimDecision {
        PASS, CLAIM_PUNG, CLAIM_MAHJONG
    }

    private final Random rng;

    public AIPlayerLogic(Random rng) {
        this.rng = rng;
    }

    /** Choose which concealed tile to discard (lowest keep-value). */
    public Tile chooseDiscard(Hand hand, Wind seatWind, Wind roundWind) {
        List<Tile> tiles = hand.getTiles();
        if (tiles.isEmpty()) {
            return null;
        }
        Tile worst = null;
        int worstScore = Integer.MAX_VALUE;
        for (int i = 0; i < tiles.size(); i++) {
            Tile t = tiles.get(i);
            int score = keepValue(t, hand, seatWind, roundWind);
            if (score < worstScore) {
                worstScore = score;
                worst = t;
            }
        }
        return worst;
    }

    /** Higher = more worth keeping. */
    private int keepValue(Tile t, Hand hand, Wind seatWind, Wind roundWind) {
        int score = 0;
        int same = hand.count(t);
        if (same >= 3) {
            score += 100;        // already a triplet, keep
        } else if (same == 2) {
            score += 40;         // a pair, useful
        }

        if (t.isSuited()) {
            // run potential: count neighbours within two ranks
            int r = t.getRank();
            TileSuit s = t.getSuit();
            if (r >= 2 && hand.count(Tile.suited(s, r - 1)) > 0) score += 12;
            if (r >= 3 && hand.count(Tile.suited(s, r - 2)) > 0) score += 6;
            if (r <= 8 && hand.count(Tile.suited(s, r + 1)) > 0) score += 12;
            if (r <= 7 && hand.count(Tile.suited(s, r + 2)) > 0) score += 6;
            // middle tiles are more flexible than terminals
            score += (r >= 3 && r <= 7) ? 4 : 1;
        } else {
            // honours: valuable if they are our own/round wind or a dragon,
            // but a lone honour is the first thing to throw
            boolean valuable = t.getSuit() == TileSuit.DRAGON
                    || (t.getWind() == seatWind)
                    || (t.getWind() == roundWind);
            if (same == 1) {
                score += valuable ? 3 : 0;
            } else {
                score += valuable ? 8 : 4;
            }
        }
        return score;
    }

    /** Decide what (if anything) to claim from a discard. */
    public ClaimDecision decideClaim(Hand hand, Tile discard, Wind seatWind, Wind roundWind) {
        if (ClaimResolver.canMahjong(hand, discard)) {
            return ClaimDecision.CLAIM_MAHJONG;
        }
        if (ClaimResolver.canPung(hand, discard)) {
            boolean honour = discard.isHonour();
            boolean valuableHonour = discard.getSuit() == TileSuit.DRAGON
                    || discard.getWind() == seatWind
                    || discard.getWind() == roundWind;
            boolean alreadyMelding = !hand.getMelds().isEmpty();
            // Pung valuable honours always; otherwise only when already committed
            // to an exposed hand, or occasionally for variety.
            if (valuableHonour || alreadyMelding) {
                return ClaimDecision.CLAIM_PUNG;
            }
            if (honour) {
                return ClaimDecision.CLAIM_PUNG;
            }
            if (rng.nextInt(100) < 30) {
                return ClaimDecision.CLAIM_PUNG;
            }
        }
        return ClaimDecision.PASS;
    }
}
