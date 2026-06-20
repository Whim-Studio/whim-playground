package com.tiwa.mahjong.api;

import java.util.List;

/**
 * The mutable game state seam between Task 1 (which owns the concrete {@code GameState}
 * implementation, setup, dealing and dice) and Task 2 (which drives the turn/claim loop).
 *
 * <p>Turn order is counter-clockwise; tiles are drawn clockwise from the {@link #getWall()}.</p>
 */
public interface GameContext {

    /** The four seated players, index 0-3. */
    List<? extends PlayerView> getPlayers();

    Wall getWall();

    /** Wind of the current round (starts East, rotates clockwise each round). */
    Wind getRoundWind();

    /** Seat index of the dealer (East) for this hand. */
    int getDealerIndex();

    int getCurrentPlayerIndex();

    void setCurrentPlayerIndex(int seatIndex);

    /** The shared discard pile, in chronological order (primary evidence for disputes). */
    List<Tile> getDiscardPile();
}
