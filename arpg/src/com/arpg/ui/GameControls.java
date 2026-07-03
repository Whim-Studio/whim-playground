package com.arpg.ui;

/**
 * Engine-level operations that are NOT {@link com.arpg.model.PlayerAction}s
 * (save/load/tick). {@link MainFrame} implements this by delegating to the
 * {@code GameEngine} facade; panels invoke it without knowing the engine.
 */
public interface GameControls {
    /** Advance the simulation by one tick. */
    void tick();

    /** Prompt for a file and save the game. */
    void saveGame();

    /** Prompt for a file and load a game. */
    void loadGame();
}
