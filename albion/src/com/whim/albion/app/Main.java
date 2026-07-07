package com.whim.albion.app;

import com.whim.albion.api.ModelFactory;
import com.whim.albion.data.AlbionModelFactory;
import com.whim.albion.engine.GameEngine;
import com.whim.albion.ui.GameFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Production entry point for the Albion recreation. Wires the three parallel-built
 * layers together across the {@code com.whim.albion.api} seam only:
 * <ul>
 *   <li>{@link AlbionModelFactory} — model &amp; content (Task 1)</li>
 *   <li>{@link GameEngine} — flow, combat, dialogue, persistence (Task 2)</li>
 *   <li>{@link GameFrame} — Swing UI &amp; rendering (Task 3)</li>
 * </ul>
 * The engine starts on the TITLE screen; the player picks "New Game" to begin.
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    /* fall back to cross-platform L&F */
                }
                ModelFactory factory = new AlbionModelFactory();
                GameEngine engine = new GameEngine(factory);
                GameFrame frame = new GameFrame(engine);
                frame.setVisible(true);
            }
        });
    }
}
