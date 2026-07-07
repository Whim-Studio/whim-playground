package com.whim.albion.ui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.whim.albion.api.GameController;

/**
 * Temporary launcher so a human can visually verify the UI shell standalone, driven by the
 * dev {@link StubController}. The production entry point is the orchestrator's {@code app.Main},
 * which injects a real {@code GameEngine} into {@link GameFrame} instead. Safe to delete after
 * integration.
 *
 * <p>Run: {@code mvn -q compile exec:java -Dexec.mainClass=com.whim.albion.ui.UiMain}
 * or after building: {@code java -cp target/classes com.whim.albion.ui.UiMain}.
 */
public final class UiMain {

    private UiMain() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
                catch (Exception ignored) { /* fall back to default L&F */ }
                GameController controller = new StubController();
                GameFrame frame = new GameFrame(controller);
                frame.setVisible(true);
            }
        });
    }
}
