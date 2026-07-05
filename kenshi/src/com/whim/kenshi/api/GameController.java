package com.whim.kenshi.api;

import java.util.List;

/**
 * The single seam between the UI and the simulation. The UI holds a reference of
 * this type and never touches engine/domain classes directly.
 *
 * Threading contract: {@link #state()} may be called from the Swing EDT at any
 * time and must return a coherent, non-blocking snapshot. All command methods
 * are also called from the EDT; the engine is responsible for applying them
 * safely on its own tick thread (e.g. via a command queue). Implementations must
 * never block the EDT.
 */
public interface GameController {

    // ---- snapshot -------------------------------------------------------
    /** Coherent read-only snapshot of the world for the current frame. */
    Views.GameStateView state();

    // ---- lifecycle ------------------------------------------------------
    /** Build a fresh world from the given seed and reset the clock. */
    void newGame(long seed);
    /** Start the background tick loop (idempotent). */
    void start();
    /** Stop the background tick loop and release its thread. */
    void stop();

    // ---- real-time-with-pause ------------------------------------------
    void setPaused(boolean paused);
    boolean isPaused();
    void togglePause();
    /** Set the game-speed multiplier (e.g. 1, 2, 4). Ignored while paused. */
    void setGameSpeed(int multiplier);

    // ---- selection ------------------------------------------------------
    /** Replace the current selection with these player-controlled character ids. */
    void setSelection(List<String> characterIds);

    // ---- orders (apply to the given ids; usually the current selection) --
    /** Order a move to a world position. */
    void orderMove(List<String> characterIds, double worldX, double worldY);
    /** Order an attack on a target character. */
    void orderAttack(List<String> characterIds, String targetId);
    /** Order interaction with a world node (enter town, etc.). */
    void orderInteract(List<String> characterIds, String nodeId);
}
