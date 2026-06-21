package com.midnight.ui;

import java.awt.GraphicsEnvironment;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point for the Lords of Midnight Swing adaptation.
 *
 * <p>The whole game is driven through {@link com.midnight.core.GameState}; the
 * computer opponent (Doomdark) comes from {@link com.midnight.ai.DoomdarkAI}.
 * This class only spins up the Swing frame on the Event Dispatch Thread &mdash;
 * it never re-implements any rule.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("The Lords of Midnight UI requires a graphical display; running headless. "
                    + "Launch this on a desktop environment to play.");
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // Cross-platform look and feel is a fine fallback.
                }
                MidnightFrame frame = new MidnightFrame();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
