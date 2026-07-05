package com.whim.populous.api;

import com.whim.populous.api.Enums.GodPower;

/**
 * THE seam between the UI (Task 3) and the simulation engine (Task 2). The UI
 * holds a reference of this type and nothing else from the engine. Task 2's
 * engine implements this; Task 3's dev {@code StubController} also implements it
 * so the UI can be built and run before the engine lands.
 *
 * Threading contract:
 *  - The engine runs its own background simulation thread (~30 ticks/sec).
 *  - {@link #state()} returns a snapshot-consistent view safe to read from the
 *    Swing EDT. Implementations must not tear across a read.
 *  - Player action methods (select/primaryClick/secondaryClick/castGlobal) are
 *    called from the EDT and must be thread-safe against the sim thread.
 *  - The engine notifies the UI to repaint via {@link ChangeListener}; the UI
 *    marshals the repaint onto the EDT itself.
 */
public interface GameController {

    /** Snapshot of current game state for rendering. Never null. */
    Views.GameStateView state();

    /** Arm a power for subsequent map clicks (RAISE_LAND by default). */
    void selectPower(GodPower power);

    /**
     * Left-click on a map tile. If the armed power is targeted it is cast here
     * (RAISE_LAND raises the tile). Costs the player's mana per the power.
     */
    ActionResult primaryClick(int col, int row);

    /**
     * Right-click on a map tile. Always lowers land at the tile regardless of
     * the armed power (classic Populous right-drag-to-lower shortcut).
     */
    ActionResult secondaryClick(int col, int row);

    /** Cast a global (non-targeted) power such as FLOOD or ARMAGEDDON. */
    ActionResult castGlobal(GodPower power);

    /** Start / stop the background simulation loop. */
    void start();
    void stop();

    /** Reset and begin a fresh game with the given seed. */
    void newGame(long seed);

    /** Advance the simulation exactly one tick (for tests / stub). */
    void tickOnce();

    void addChangeListener(ChangeListener listener);
    void removeChangeListener(ChangeListener listener);

    /** Fired (off the EDT) whenever state changed enough to warrant a repaint. */
    interface ChangeListener {
        void onStateChanged();
    }
}
