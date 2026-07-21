package com.railroad;

import com.railroad.ui.GameFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point. Launches the Swing Railroad Tycoon (Phase 1) client
 * on the Event Dispatch Thread.
 *
 * <p>The world seed is fixed by default so layouts are reproducible for QA. Pass
 * {@code -Drailroad.seed=<n>} on the command line to generate a different map.
 */
public final class Main {

    private static final long DEFAULT_SEED = 42L;

    public static void main(String[] args) {
        final long seed = resolveSeed();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // Fall back to the default look and feel.
                }
                new GameFrame(seed).setVisible(true);
            }
        });
    }

    private static long resolveSeed() {
        String prop = System.getProperty("railroad.seed");
        if (prop != null) {
            try {
                return Long.parseLong(prop.trim());
            } catch (NumberFormatException ignored) {
                // fall through to default
            }
        }
        return DEFAULT_SEED;
    }
}
