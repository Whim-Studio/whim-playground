package com.whim.shinobi.api;

/**
 * The ONE seam between the Swing UI (Task 3) and the simulation engine (Task 2).
 *
 * Threading contract: input methods are called on the Swing EDT and must be
 * cheap + thread-safe (record intent, e.g. set a volatile/atomic flag). The
 * engine consumes that intent on its own background tick thread. {@link #state()}
 * returns a coherent, thread-safe snapshot the UI can read on the EDT.
 *
 * DO NOT modify — Task 2 implements this; Task 3 depends on it. Task 3 develops
 * against a local stub implementing this same interface.
 */
public interface GameController {

    /** Coherent world snapshot for rendering. Safe to call from the EDT. */
    Views.GameStateView state();

    // ---- Held movement (UI calls on key press = true, key release = false) ----
    void setLeft(boolean held);
    void setRight(boolean held);
    void setCrouch(boolean held);

    // ---- Discrete actions (UI calls once per key press) ----
    /** Vertical jump on the current plane. */
    void jump();
    /** Leap between LOWER and UPPER paths (classic plane-jump). */
    void shiftPlane();
    /** Context-sensitive attack: melee if closest same-plane enemy within
     *  {@link Config#MELEE_RANGE}, otherwise throw the current weapon. */
    void attack();
    /** Screen-clearing Ninjutsu magic (consumes one charge if available). */
    void ninjutsu();

    // ---- Lifecycle ----
    /** Build/reset the first level and prepare to run. */
    void newGame();
    /** Start the background tick loop. */
    void start();
    /** Stop the background tick loop and release its thread. */
    void stop();
    void togglePause();
}
