package com.whim.cardwoven.app;

import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.whim.cardwoven.api.Enums.Faction;
import com.whim.cardwoven.api.GameController;
import com.whim.cardwoven.domain.GameState;
import com.whim.cardwoven.engine.GameEngine;
import com.whim.cardwoven.ui.GameFrame;

/**
 * Entry point for Cardwoven Empires. Wires the real engine (Task 2) — backed by
 * the domain model (Task 1) — into the Swing UI (Task 3), swapping out the
 * dev-only StubController the UI ships with.
 *
 * Data flow: UI renders {@code controller.state()} and calls {@link GameController}
 * actions; the {@link GameEngine} mutates the {@link GameState}; the UI re-reads
 * and repaints. No layer reaches across the {@code api} seam.
 *
 * Run headful:   java com.whim.cardwoven.app.Main
 * Run headless:  java -Djava.awt.headless=true com.whim.cardwoven.app.Main   (self-check)
 *
 * Java 8 only.
 */
public final class Main {

    private Main() {}

    /** Faction the human starts as; the in-game "New Game" picker can change it. */
    private static final Faction DEFAULT_FACTION = Faction.LANDS_OF_THE_KING;

    public static void main(String[] args) {
        // A fresh seed per launch keeps games varied; determinism within a game
        // is preserved by the engine's own seeded Random.
        final long seed = System.currentTimeMillis();
        final GameState initial = GameState.create(DEFAULT_FACTION, seed);
        final GameController controller = new GameEngine(initial);

        final boolean headless = GraphicsEnvironment.isHeadless()
                || Boolean.getBoolean("java.awt.headless");
        if (headless) {
            runHeadlessSelfCheck(controller);
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // Fall back to the default L&F; purely cosmetic.
                }
                GameFrame frame = new GameFrame(controller);
                frame.setVisible(true);
            }
        });
    }

    /**
     * Headless smoke: exercise the real engine through the {@link GameController}
     * seam (no Swing window, which cannot exist headless) so CI/containers can
     * confirm the wired stack runs end to end.
     */
    private static void runHeadlessSelfCheck(GameController controller) {
        System.out.println("[Cardwoven Empires] headless self-check — real engine wired");
        System.out.println("  start: " + controller.state().players().size()
                + " players, "
                + controller.state().map().rows() + "x" + controller.state().map().cols()
                + " map, human=" + controller.state().currentPlayer().faction().display());
        int guard = 0;
        while (!controller.state().isGameOver() && guard < 200) {
            controller.endTurn();
            guard++;
        }
        if (controller.state().isGameOver()) {
            int w = controller.state().winnerPlayerIndex();
            System.out.println("  result: player " + w + " ("
                    + controller.state().players().get(w).faction().display()
                    + ") wins by " + controller.state().winningVictory()
                    + " on turn " + controller.state().turnNumber());
        } else {
            System.out.println("  result: no victory within " + guard + " turns (guard hit)");
        }
        System.out.println("  OK");
    }
}
