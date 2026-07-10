package com.whim.scg.app;

import com.whim.scg.api.GameController;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;

/**
 * Entry point. Boots the real engine reflectively so the shell has zero compile
 * dependency on Task 1's package; falls back to {@link StubController} when the
 * engine is absent. Runs headless-safe for CI (prints a notice instead of
 * opening a window when no display is available).
 */
public final class Main {
    private Main() {}

    public static void main(String[] args) {
        final GameController controller = bootController();

        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Star Command: Galaxies");
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

    private static GameController bootController() {
        try {
            Class<?> c = Class.forName("com.whim.scg.engine.Engine");
            return (GameController) c.getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
            System.err.println("[boot] engine not found, using stub: " + t);
            return new StubController();
        }
    }
}
