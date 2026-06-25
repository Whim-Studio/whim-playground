package com.whim.tarot.app;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.whim.tarot.ui.MainWindow;

/**
 * Entry point for the Tarot Reader app.
 *
 * Standalone Java 8 Swing application — a full 78-card Rider-Waite-Smith Tarot
 * reading engine with three classic spreads (Daily Focus, Past/Present/Future,
 * and the ten-card Celtic Cross). No external libraries.
 *
 * Run:
 *   javac -d out $(find tarot/src -name '*.java')
 *   java -cp out com.whim.tarot.app.Main
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
                new MainWindow().showApp();
            }
        });
    }
}
