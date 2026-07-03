package com.whim.powermonger.app;

import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.whim.powermonger.api.GameController;
import com.whim.powermonger.engine.GameEngine;
import com.whim.powermonger.ui.GameFrame;

/**
 * Powermonger — standalone Java 8 Swing adaptation.
 *
 * Wires the real simulation engine (Task 2) to the Swing UI (Task 3) through the
 * {@code api.GameController} seam. Zero external dependencies; all graphics are
 * procedural {@code Graphics2D}.
 *
 * Run (after `mvn -f powermonger/pom.xml compile`):
 *   java -cp powermonger/target/classes com.whim.powermonger.app.Main [seed]
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

        final GameController controller = new GameEngine(seed);

        if (GraphicsEnvironment.isHeadless()) {
            // No display available: prove the full stack wires up and advances,
            // then exit cleanly (useful for CI / container smoke checks).
            System.out.println("[Powermonger] headless: running engine for ~500ms (seed=" + seed + ")...");
            controller.start();
            try {
                Thread.sleep(500L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            controller.stop();
            System.out.println("[Powermonger] headless OK: tick=" + controller.state().tickCount()
                    + ", captains=" + controller.state().captains().size()
                    + ", towns=" + controller.state().towns().size()
                    + ", balanceOfPower=" + controller.state().balanceOfPower());
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // system L&F is optional
                }
                GameFrame frame = new GameFrame(controller);
                frame.launch();
            }
        });
    }
}
