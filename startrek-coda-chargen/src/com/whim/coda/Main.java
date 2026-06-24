package com.whim.coda;

import com.whim.coda.ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point for the Star Trek Roleplaying Game (Coda System) character creator.
 *
 * <p>Standalone Java 8 / Swing application. No external libraries, no build tool required:
 * <pre>
 *   find startrek-coda-chargen/src -name '*.java' &gt; /tmp/srcs
 *   javac -d /tmp/out @/tmp/srcs
 *   java -cp /tmp/out com.whim.coda.Main
 * </pre>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to the cross-platform look and feel.
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new MainFrame().setVisible(true);
            }
        });
    }
}
