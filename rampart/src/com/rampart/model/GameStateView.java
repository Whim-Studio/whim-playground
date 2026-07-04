package com.rampart.model;

import java.util.List;

/**
 * The top-level read-only snapshot the UI polls each frame. Concrete class:
 * {@link GameState}. Everything the HUD and renderer need is reachable from here;
 * the UI never touches concrete engine/model classes directly.
 */
public interface GameStateView {
    /** @return the current lifecycle phase */
    Phase phase();

    /** @return the 1-based round number */
    int round();

    /** @return milliseconds remaining in the current timed phase */
    long timerRemainingMillis();

    /** @return the current score */
    long score();

    /** @return remaining lives */
    int lives();

    /** @return the fraction (0..1) of buildable land currently enclosed */
    double territoryFraction();

    /** @return number of currently enclosed, living castles */
    int enclosedCastleCount();

    /** @return the read-only playfield grid */
    GridView grid();

    /** @return an unmodifiable list of the castles in play */
    List<? extends CastleView> castles();

    /** @return an unmodifiable list of placed cannons */
    List<? extends CannonView> cannons();

    /** @return an unmodifiable list of enemy ships */
    List<? extends ShipView> ships();

    /** @return the wall piece being placed this REPAIR phase, or {@code null} */
    WallPieceView currentPiece();

    /**
     * @return an unmodifiable list of upcoming queued wall pieces (preview),
     *         possibly empty
     */
    List<? extends WallPieceView> queuedPieces();

    /** @return number of cannons still available to place this BUILD phase */
    int cannonsRemainingToPlace();

    /** @return {@code true} once the game has ended */
    boolean gameOver();
}
