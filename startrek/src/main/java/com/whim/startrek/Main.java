package com.whim.startrek;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.whim.startrek.ui.MainFrame;

/**
 * Entry point for "StarTrek: A New Beginning" — a standalone Java 8 / Swing hybrid
 * strategy + real-time combat game (a clone of <i>Star Trek: Birth of the Federation</i>).
 *
 * <p>Authored by the orchestrator during consolidation. It wires nothing itself: the
 * {@link MainFrame} no-arg constructor builds a ready-to-play galaxy via
 * {@code GameFactory.newGame(...)} and the live engine services (economy, fleet AI,
 * Borg, RTS battles). This class only launches that frame on the Swing EDT.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // Fall back to the default look and feel; not fatal.
                }
                new MainFrame().setVisible(true);
            }
        });
    }
}
