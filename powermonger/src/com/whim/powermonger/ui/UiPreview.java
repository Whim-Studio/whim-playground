package com.whim.powermonger.ui;

import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;

import com.whim.powermonger.api.GameController;

/**
 * Dev entry point: launches {@link GameFrame} against the {@link StubController}
 * so the UI package can be built and run standalone, before the real engine is
 * wired in by {@code app.Main}.
 */
public final class UiPreview {
    private UiPreview() {}

    public static void main(String[] args) {
        final GameController controller = new StubController();
        if (GraphicsEnvironment.isHeadless()) {
            // No display: construct the frame to prove there are no ctor errors,
            // run a few animation steps, then exit cleanly.
            System.out.println("[UiPreview] headless: building panels against StubController...");
            // JFrame can't be created headless, but the panels can — exercise
            // their constructors to prove there are no wiring errors.
            MapPanel map = new MapPanel(controller);
            new MiniMapPanel(controller, map);
            new ConsolePanel(controller);
            new BalancePanel(controller);
            controller.start();
            try { Thread.sleep(300); } catch (InterruptedException ignored) { }
            controller.stop();
            System.out.println("[UiPreview] headless OK: state tick="
                    + controller.state().tickCount()
                    + ", captains=" + controller.state().captains().size()
                    + ", towns=" + controller.state().towns().size());
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                GameFrame frame = new GameFrame(controller);
                frame.launch();
            }
        });
    }
}
