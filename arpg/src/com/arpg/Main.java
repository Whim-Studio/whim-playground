package com.arpg;

import com.arpg.engine.GameEngine;
import com.arpg.ui.MainFrame;

import javax.swing.SwingUtilities;

/**
 * Application entry point. Constructs the headless {@link GameEngine} and launches the Swing
 * {@link MainFrame} on the Event Dispatch Thread. The frame registers its own
 * {@code GameEventListener} with the engine, so all cross-layer wiring happens here.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        final long seed = parseSeed(args);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                GameEngine engine = new GameEngine(seed);
                MainFrame frame = new MainFrame(engine);
                frame.setVisible(true);
            }
        });
    }

    /** Optional first argument: a numeric RNG seed for deterministic runs. */
    private static long parseSeed(String[] args) {
        if (args != null && args.length > 0) {
            try {
                return Long.parseLong(args[0].trim());
            } catch (NumberFormatException ignored) {
                // fall through to a fixed default seed
            }
        }
        return 20260703L;
    }
}
