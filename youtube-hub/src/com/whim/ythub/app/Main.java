package com.whim.ythub.app;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.whim.ythub.ui.HubFrame;

/**
 * Application entry point for the YouTube Hub &amp; Launcher.
 *
 * <p>Sets the platform system Look-and-Feel (best effort) and launches the main
 * {@link HubFrame} on the Event Dispatch Thread via
 * {@link SwingUtilities#invokeLater}. Strict Java 8: no {@code var}, no text
 * blocks, no post-8 APIs.</p>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            // Fall back to the default cross-platform L&F; not fatal.
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new HubFrame().setVisible(true);
            }
        });
    }
}
