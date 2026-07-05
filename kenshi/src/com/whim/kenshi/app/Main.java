package com.whim.kenshi.app;

import com.whim.kenshi.api.GameController;
import com.whim.kenshi.engine.GameEngine;
import com.whim.kenshi.ui.GameFrame;

/**
 * Shipping entry point for the Kenshi demake. Wires the real simulation
 * ({@link GameEngine}) to the Swing presentation layer ({@link GameFrame}) via
 * the {@link GameController} seam and starts the world.
 *
 * <p>The engine owns a background tick thread (Real-Time-with-Pause); the UI
 * polls {@code controller.state()} on the EDT and repaints. Neither side touches
 * the other's classes — everything crosses through {@code GameController} and the
 * read-only {@code Views}.</p>
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        long seed = 42L;
        if (args.length > 0) {
            try {
                seed = Long.parseLong(args[0].trim());
            } catch (NumberFormatException ignored) {
                // fall back to the default seed
            }
        }

        final GameController controller = new GameEngine();
        controller.newGame(seed);
        controller.start();

        // GameFrame.show marshals frame construction onto the EDT and starts the
        // repaint timer; the engine tick thread is already running.
        GameFrame.show(controller);
    }
}
