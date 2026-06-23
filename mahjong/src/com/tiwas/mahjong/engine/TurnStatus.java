package com.tiwas.mahjong.engine;

import com.tiwas.mahjong.model.Tile;

/**
 * What the engine needs from (or is telling) the UI after {@link GameEngine#advance()}.
 * The UI loops: call advance(), act on the status, feed input back, repeat.
 */
public final class TurnStatus {

    public enum Kind {
        AWAIT_HUMAN_DRAW,    // human must draw from the wall
        AWAIT_HUMAN_DISCARD, // human must discard (or kong / declare mahjong)
        AWAIT_HUMAN_CLAIM,   // human may claim the last discard
        HAND_OVER            // the hand finished; see GameEngine.getLastResult()
    }

    public final Kind kind;

    // For AWAIT_HUMAN_CLAIM:
    public Tile claimableDiscard;
    public int discardBy = -1;
    public boolean canPung;
    public boolean canMahjong;

    public TurnStatus(Kind kind) {
        this.kind = kind;
    }
}
