package com.tiwas.mahjong.model;

/**
 * The turn-level actions the engine exposes to the UI. The UI only ever talks to
 * the engine through this contract (plus {@link Scorable}); it never touches
 * engine internals.
 */
public interface Playable {

    /** Draw a tile from the wall for the given player (handling bonus replacement). */
    Tile drawTile(int playerIndex);

    /** Discard the given tile from the player's hand, ending their turn. */
    void discardTile(int playerIndex, Tile tile);

    /** Have a player claim the last discard to form a meld of the given type. */
    boolean claimMeld(int playerIndex, MeldType type);

    /** Have a player declare mahjong (on a discard or self-draw). */
    boolean declareMahjong(int playerIndex);
}
