package com.heroquest;

import com.heroquest.ui.GameFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Application entry point. Launches the Swing HeroQuest client on the EDT. */
public final class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // Fall back to the default look and feel.
                }
                new GameFrame().setVisible(true);
            }
        });
    }
}
