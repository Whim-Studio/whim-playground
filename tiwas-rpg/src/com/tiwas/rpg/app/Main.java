package com.tiwas.rpg.app;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.tiwas.rpg.ui.MainFrame;

/**
 * Entry point for the Tiwas RPG — Demo Version desktop app. Sets the system
 * look-and-feel and builds the {@link MainFrame} on the Event Dispatch Thread.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // fall back to the default cross-platform look and feel
                }
                new MainFrame().showFrame();
            }
        });
    }
}
