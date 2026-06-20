package com.tiwa.mahjong.api;

import java.util.List;

/**
 * Read-only view of a player's state, consumed by the rules engine (Task 2) and the
 * scoring calculator (Task 3). The concrete player + mutation API lives in Task 1.
 */
public interface PlayerView {

    /** 0-3 seat index. Seat 0 is the dealer (East) at deal time. */
    int getSeatIndex();

    Wind getSeatWind();

    /** Tiles still hidden in hand (not yet melded). */
    List<Tile> getConcealedTiles();

    /** Exposed and concealed melds declared by this player. */
    List<Meld> getMelds();

    /** Flowers/Seasons revealed by this player (4 points each; never melds). */
    List<Tile> getBonusTiles();

    /**
     * True if this player has claimed ANY tile from the discard pile during the current hand.
     * Drawing from the wall, drawing replacements, and revealing bonus tiles do NOT set this.
     * Required to validate the Fully Concealed Hand bonus (Section 5).
     */
    boolean hasClaimedDiscardThisHand();
}
