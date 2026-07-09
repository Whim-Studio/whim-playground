package com.whim.digitallife;

import com.whim.digitallife.ui.QuizFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point for the "thisisyourdigitallife" personality quiz.
 *
 * <p>This is an original, fully self-contained local desktop app. It does not
 * connect to Facebook or any social network, performs no networking of any kind,
 * and only ever touches the single local user's own answers on their own machine.</p>
 */
public final class Main {

    private Main() {
        // Entry-point holder; not instantiable.
    }

    /**
     * Launches the Swing UI on the Event Dispatch Thread.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        // Use the platform look-and-feel when available; fall back silently.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Any failure here is non-fatal; the cross-platform L&F is fine.
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new QuizFrame().setVisible(true);
            }
        });
    }
}
