package com.janggi.ui;

import java.awt.GraphicsEnvironment;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point for the Janggi (Korean Chess) Swing application.
 *
 * <p>The whole game is driven through {@link com.janggi.core.GameState} and the
 * computer player is supplied by {@link com.janggi.ai.MinimaxAI}. This class only
 * wires up the Swing frame on the Event Dispatch Thread.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            // A headless container (e.g. CI) cannot show a window. Fail gracefully
            // instead of throwing a HeadlessException out of the EDT.
            System.err.println("Janggi UI requires a graphical display; running headless. "
                    + "Launch this on a desktop environment to play.");
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // Fall back to the cross-platform look and feel.
                }
                JanggiFrame frame = new JanggiFrame();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
