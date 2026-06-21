package com.whim.hbdi;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.whim.hbdi.ui.MainFrame;

/**
 * Entry point for the standalone HBDI (Herrmann Brain Dominance Instrument)
 * Swing survey application. Builds and shows the wizard on the EDT.
 *
 * Java 8 only; no external dependencies.
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
                    // Fall back to the default look and feel.
                }
                new MainFrame().setVisible(true);
            }
        });
    }
}
