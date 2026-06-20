package com.tiwa.mahjong.engine;

import com.tiwa.mahjong.api.Meld;
import com.tiwa.mahjong.api.PlayerView;

/**
 * Verifies the Fully Concealed Hand bonus (Section 5).
 *
 * <p>A hand is fully concealed if it was formed without EVER claiming a tile from the discard pile
 * during the hand. Drawing from the wall, drawing kong/flower replacements, and revealing
 * flowers/seasons do NOT disqualify it; ANY discard claim does.</p>
 *
 * <p>Evidence used: {@link PlayerView#hasClaimedDiscardThisHand()} (authoritative) plus the meld
 * history - any exposed meld means a tile was taken from a discard, which is a belt-and-braces
 * cross-check of the flag.</p>
 */
public final class FullyConcealedVerifier {

    /** Verify the bonus for one player. */
    public FullyConcealedResult verify(PlayerView player) {
        if (player == null) {
            throw new IllegalArgumentException("player must not be null");
        }
        if (player.hasClaimedDiscardThisHand()) {
            return new FullyConcealedResult(false, "player claimed a tile from the discard pile this hand");
        }
        for (Meld meld : player.getMelds()) {
            if (!meld.isConcealed()) {
                return new FullyConcealedResult(false, "player has an exposed meld: " + meld.getType());
            }
        }
        return new FullyConcealedResult(true, "no discard claims and all melds concealed");
    }
}
