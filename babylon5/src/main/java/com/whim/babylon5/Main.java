package com.whim.babylon5;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.whim.babylon5.domain.GameFactory;
import com.whim.babylon5.domain.GameState;
import com.whim.babylon5.engine.GameEngine;
import com.whim.babylon5.ui.MainWindow;

/**
 * Entry point for the standalone Babylon 5 CCG prototype: one human player versus
 * exactly three AI opponents. Wiring only — all rules live in {@code engine}, all
 * state in {@code domain}, all presentation in {@code ui}.
 *
 * <p>Pass a numeric argument to seed a deterministic game (handy for reproducing a
 * board); otherwise the clock seeds it.
 *
 * <pre>
 *   javac --release 8 -d out $(find babylon5/src/main/java -name '*.java')
 *   cp -r babylon5/src/main/resources/* out/
 *   java -cp out com.whim.babylon5.Main
 * </pre>
 */
public final class Main {

    private Main() { }

    public static void main(String[] args) {
        long seed = System.currentTimeMillis();
        if (args.length > 0) {
            try {
                seed = Long.parseLong(args[0].trim());
            } catch (NumberFormatException ignored) {
                // fall back to the clock-based seed
            }
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // default L&F is fine
        }

        final GameState state = GameFactory.newStandardGame(seed);
        final GameEngine engine = new GameEngine(state);
        SwingUtilities.invokeLater(() -> new MainWindow(engine).start());
    }
}
