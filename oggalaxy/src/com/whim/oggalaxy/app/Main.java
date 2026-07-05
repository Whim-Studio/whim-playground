package com.whim.oggalaxy.app;

import com.whim.oggalaxy.engine.GameEngine;
import com.whim.oggalaxy.ui.StartScreen;
import com.whim.oggalaxy.ui.UiPreview;

import javax.swing.SwingUtilities;

/**
 * Production entry point for OG Galaxy.
 *
 * Wires the real {@link GameEngine} (which owns its own background tick thread) to the
 * {@link StartScreen}. The start screen collects the new-game setup, calls
 * {@code newGame(...)} and hands the same controller to the {@code MainFrame}. No preview
 * clock driver is used here — the engine advances time itself; the UI is strictly
 * poll-and-repaint.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                UiPreview.applyDarkTheme();          // shared dark-space Swing theme
                GameEngine engine = new GameEngine(); // real simulation; drop-in for DemoController
                new StartScreen(engine).setVisible(true);
            }
        });
    }
}
