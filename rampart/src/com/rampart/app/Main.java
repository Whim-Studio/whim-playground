package com.rampart.app;

import javax.swing.SwingUtilities;

import com.rampart.engine.GameEngine;
import com.rampart.ui.GameFrame;

/**
 * Playable entry point for the standalone Rampart (1990) recreation.
 *
 * <p>Wires the real headless {@link GameEngine} (Task 2, implements
 * {@code com.rampart.engine.GameApi}) to the Swing {@link GameFrame} (Task 3) on
 * the Event Dispatch Thread. {@link GameFrame#launch()} starts a new game and the
 * ~30&nbsp;fps Swing-timer loop that calls {@code tick(dt)} then repaints. This is
 * the only class that references both the engine and the UI concretely; it lives
 * in {@code com.rampart.app} so the strict {@code ui -> engine -> model} direction
 * inside the three feature packages is never violated.</p>
 *
 * <pre>
 * Build: cd rampart &amp;&amp; javac --release 8 -d out $(find src -name '*.java')
 * Run:   java -cp out com.rampart.app.Main
 * </pre>
 */
public final class Main {

    private Main() {
    }

    /**
     * Launches the game on the Swing EDT.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                GameEngine engine = new GameEngine();
                GameFrame frame = new GameFrame(engine);
                frame.launch();
            }
        });
    }
}
