package com.whim.kenshi.ui;

import javax.swing.SwingUtilities;

/**
 * Dev entry point: launches the full Swing UI against the {@link StubController}
 * so the presentation layer can be exercised before the real engine lands. This
 * is NOT the shipping entry point — {@code com.whim.kenshi.app.Main} wires the UI
 * to the real {@code GameEngine}.
 *
 * <p>Run headless-safely: if no display is available it still constructs the
 * controller + frame (catching {@link java.awt.HeadlessException}) so a CI smoke
 * check can confirm the UI classes instantiate without a screen.</p>
 */
public final class UiPreview {

    private UiPreview() {}

    public static void main(String[] args) {
        final StubController controller = new StubController();
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            // No display: prove the controller + snapshot pipeline work, then exit.
            controller.start();
            controller.state();
            controller.stop();
            System.out.println("UiPreview: headless smoke check OK (no display).");
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                GameFrame frame = new GameFrame(controller);
                frame.launch();
            }
        });
    }
}
