package com.whim.firetop.ui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point. Launches the Swing UI on the event dispatch thread.
 */
public final class Main {

    private Main() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // Fall back to cross-platform L&F.
                }
                GameFrame frame = new GameFrame();
                frame.setVisible(true);
            }
        });
    }
}
