package com.whim.b5wars;

import com.whim.b5wars.data.DataLoader;
import com.whim.b5wars.model.Faction;
import com.whim.b5wars.model.Scenario;
import com.whim.b5wars.ui.GameController;
import com.whim.b5wars.ui.MainWindow;

import java.awt.GraphicsEnvironment;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point for the Java 8 / Swing recreation of <em>Babylon 5 Wars</em>.
 *
 * <p>Loads the bundled factions and the {@code /scenarios/border-skirmish.json} 1-on-1 duel,
 * builds a {@link GameController} over a fixed-seed {@code GameState}, and opens the main window
 * for hot-seat 2-player play (Side A vs Side B).
 *
 * <p>Headless-safe: under {@code -Djava.awt.headless=true} (or with no display) it constructs the
 * state and controller, runs a light sanity check, prints a summary, and exits WITHOUT opening a
 * window — so it can be exercised in CI.
 */
public final class Main {

    /** Fixed seed → deterministic dice, so runs and tests are reproducible. */
    public static final long DEFAULT_SEED = 424242L;

    private static final String SCENARIO = "/scenarios/border-skirmish.json";

    private Main() {
    }

    public static void main(String[] args) {
        List<Faction> factions = DataLoader.loadFactions();
        Scenario scenario = DataLoader.loadScenario(SCENARIO);

        if (GraphicsEnvironment.isHeadless()) {
            runHeadlessSanity(scenario, factions);
            return;
        }

        final GameController controller = new GameController(scenario, factions, DEFAULT_SEED);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // fall back to default L&F
                }
                MainWindow window = new MainWindow(controller);
                window.setVisible(true);
            }
        });
    }

    /**
     * Build the controller and (without a display) confirm the scenario, factions and ship state
     * are wired up. Never opens a frame. Prints a one-line-per-ship summary.
     */
    private static void runHeadlessSanity(Scenario scenario, List<Faction> factions) {
        GameController controller = new GameController(scenario, factions, DEFAULT_SEED);
        int ships = controller.state().getShips().size();
        System.out.println("[b5wars] Headless sanity: scenario '" + scenario.getName()
                + "', " + factions.size() + " factions, " + ships + " ships.");
        for (com.whim.b5wars.model.Ship s : controller.state().getShips()) {
            System.out.println("  - " + s.getType().getName() + " (Side " + s.getSide()
                    + ") @ " + s.getPos() + " facing " + s.getFacing() + " speed " + s.getSpeed());
        }
        System.out.println("[b5wars] Turn " + controller.state().getTurn()
                + " phase " + controller.state().getPhase()
                + ". UI construction skipped (headless).");
    }
}
