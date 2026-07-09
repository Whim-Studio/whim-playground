package com.whim.alganon.app;

import com.whim.alganon.api.GameController;
import com.whim.alganon.api.ModelFactory;
import com.whim.alganon.engine.GameEngine;
import com.whim.alganon.model.AlganonModelFactory;
import com.whim.alganon.ui.GameFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Production entry point for the Alganon single-player recreation. Wires the three
 * parallel-built layers together across the {@code com.whim.alganon.api} seam only:
 * <ul>
 *   <li>{@link AlganonModelFactory} — model &amp; content (Task 1)</li>
 *   <li>{@link GameEngine} — flow, combat, quests, study, crafting, faction-war, save/load (Task 2)</li>
 *   <li>{@link GameFrame} — Swing UI &amp; procedural rendering (Task 3)</li>
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
                ModelFactory factory = new AlganonModelFactory();
                GameController engine = new GameEngine(factory);
                GameFrame frame = new GameFrame(engine);
                frame.setVisible(true);
            }
        });
    }
}
