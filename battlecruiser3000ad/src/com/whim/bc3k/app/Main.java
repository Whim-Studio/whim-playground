package com.whim.bc3k.app;

import com.whim.bc3k.api.GameController;
import com.whim.bc3k.engine.Engine;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

/**
 * Entry point. Boots the engine and opens the bridge. Runs headless-safe for CI
 * (prints a notice instead of opening a window when no display is available).
 */
public final class Main {
    private Main() {}

    public static void main(String[] args) {
        final GameController controller = new Engine();

        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Battlecruiser 3000AD");
            System.out.println("Controller: " + controller.getClass().getName());
            System.out.println("No display available (headless) — launch on a desktop to play.");
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                new GameFrame(controller).setVisible(true);
            }
        });
    }
}
