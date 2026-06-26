package com.whim.jutsutrainer.app;

import com.whim.jutsutrainer.data.JutsuRepository;
import com.whim.jutsutrainer.engine.JutsuService;
import com.whim.jutsutrainer.ui.MainWindow;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point for the Jutsu Database &amp; Seal Trainer.
 *
 * Wires the three packages together: the hardcoded {@link JutsuRepository}
 * (Task 1) feeds the pure-logic {@link JutsuService} (Task 2), which drives the
 * Swing {@link MainWindow} (Task 3). All UI construction happens on the EDT.
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
                JutsuService service = new JutsuService(new JutsuRepository().all());
                new MainWindow(service).setVisible(true);
            }
        });
    }
}
