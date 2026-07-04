package com.whim.shinobi.app;

import javax.swing.SwingUtilities;

import com.whim.shinobi.engine.GameEngine;
import com.whim.shinobi.ui.GameFrame;

/**
 * Playable entry point for the standalone Shinobi (1987) adaptation.
 *
 * Wires the real simulation {@link GameEngine} (background 60 Hz tick loop) to the
 * Swing {@link GameFrame} on the Event Dispatch Thread. {@link GameFrame#launch()}
 * calls {@code newGame()} + {@code start()} on the controller and starts the
 * ~60 fps repaint timer; the frame's shutdown hook stops both cleanly.
 *
 * Run:  java -cp target/classes com.whim.shinobi.app.Main
 * Build: cd shinobi && mvn -o package   (or javac --release 8 -d out $(find src -name '*.java'))
 */
public final class Main {
    private Main() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                GameEngine engine = new GameEngine();
                GameFrame frame = new GameFrame(engine);
                frame.launch();
            }
        });
    }
}
