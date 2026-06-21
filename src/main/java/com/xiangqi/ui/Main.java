package com.xiangqi.ui;

import java.awt.GraphicsEnvironment;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point for the Xiangqi (Chinese Chess) Swing application.
 *
 * <p>The whole game is driven through {@link com.xiangqi.core.GameState}; the
 * computer player comes from {@link com.xiangqi.ai.MinimaxAI} and the teaching
 * hints from {@link com.xiangqi.ai.XiangqiCoach}. This class only spins up the
 * Swing frame on the Event Dispatch Thread.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Xiangqi UI requires a graphical display; running headless. "
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
                StartupDialog.Choice choice = StartupDialog.prompt();
                if (choice == null) {
                    return; // user cancelled the startup dialog
                }
                XiangqiFrame frame = new XiangqiFrame(choice.mode, choice.humanSide);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
