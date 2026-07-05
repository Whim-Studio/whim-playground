package com.whim.populous.app;

import javax.swing.SwingUtilities;

import com.whim.populous.api.GameController;
import com.whim.populous.engine.SimulationEngine;
import com.whim.populous.ui.GameFrame;

/**
 * Ready-to-run entry point for the standalone Populous adaptation. Wires the
 * real {@link SimulationEngine} (Task 2) to the Swing {@link GameFrame} (Task 3)
 * over the shared {@code GameController} seam (Task 1's api), starts a new game,
 * and spins up the background simulation loop.
 *
 * Run:  java -cp target/classes com.whim.populous.app.Main [seed]
 */
public final class Main {

    private Main() { }

    public static void main(String[] args) {
        final long seed = parseSeed(args);

        // Build the engine off the EDT; it owns its own sim thread.
        final GameController controller = new SimulationEngine(seed);
        controller.newGame(seed);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                GameFrame frame = new GameFrame(controller);
                frame.setVisible(true);
                // Begin ticking only once the UI is realised and listening.
                controller.start();
            }
        });
    }

    private static long parseSeed(String[] args) {
        if (args != null && args.length > 0) {
            try {
                return Long.parseLong(args[0].trim());
            } catch (NumberFormatException ignored) {
                // fall through to default
            }
        }
        return 20260703L;
    }
}
