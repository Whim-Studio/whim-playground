package com.rampart.engine;

import com.rampart.model.GameStateView;
import com.rampart.model.WallPieceView;

/**
 * The single contact surface between the Swing UI (Task 3) and the game engine
 * (Task 2). The UI holds a {@code GameApi}, polls {@link #state()} on its Swing
 * repaint timer, repaints from that read-only snapshot, and forwards all user
 * input as calls on this interface. The UI never casts a snapshot to a concrete
 * engine/model class and never mutates model state directly.
 *
 * <p>Implemented by {@code com.rampart.engine.GameEngine} (Task 2). The UI also
 * ships a {@code StubGameApi} implementing this interface so the presentation
 * layer can run before the real engine lands.</p>
 *
 * <p>Dependency direction is strictly {@code ui -> engine -> model}: this
 * interface lives in the engine package, returns only {@code com.rampart.model}
 * read-only view types, and references zero Swing/AWT classes.</p>
 */
public interface GameApi {

    /** Reset everything to a fresh game: round 1, first level, {@code TITLE} phase. */
    void newGame();

    /**
     * Begin the current round, entering the BUILD (cannon-placement) phase and
     * starting its timer.
     */
    void startRound();

    /**
     * Advance all timers and the battle simulation by {@code dtMillis}. Called
     * once per Swing tick by the UI game loop. Drives phase transitions, ship
     * movement/firing, cannon reload, and round progression internally.
     *
     * @param dtMillis elapsed wall-clock milliseconds since the previous tick
     */
    void tick(long dtMillis);

    /**
     * @return the current immutable-for-the-frame {@link GameStateView} snapshot
     *         the UI renders from. Never {@code null}.
     */
    GameStateView state();

    // ---- BUILD / cannon-placement phase input --------------------------------

    /**
     * Attempt to place a cannon at grid cell {@code (col,row)} during the BUILD
     * phase. Legal only on enclosed castle territory with cannons remaining.
     *
     * @return {@code true} if the cannon was placed
     */
    boolean placeCannon(int col, int row);

    // ---- BATTLE phase input --------------------------------------------------

    /**
     * Fire a ready cannon at target cell {@code (col,row)} during the BATTLE
     * phase. Resolves trajectory/blast against ships and walls internally.
     *
     * @return {@code true} if a cannon fired (a loaded cannon was available)
     */
    boolean fireCannonAt(int col, int row);

    // ---- REPAIR phase input --------------------------------------------------

    /** Rotate the current REPAIR-phase wall piece clockwise (no-op if none). */
    void rotatePiece();

    /**
     * Drop the current wall piece so its anchor lands at {@code (col,row)}.
     * Rejected on overlap with water/walls/cannons/castles or out-of-bounds.
     *
     * @return {@code true} if the piece was placed (and the next piece dealt)
     */
    boolean placePieceAt(int col, int row);

    /** @return the wall piece currently being placed, or {@code null} if none. */
    WallPieceView currentPiece();

    // ---- phase control / lifecycle ------------------------------------------

    /** Player signals ready, ending the current timed phase before it expires. */
    void endPhaseEarly();

    /** @return {@code true} once the game has ended (no castle could be enclosed). */
    boolean isGameOver();
}
