package com.whim.stars.app;

import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.whim.stars.model.Galaxy;
import com.whim.stars.sim.TurnEngine;
import com.whim.stars.ui.MainWindow;

/**
 * Application entry point. Launches the Swing UI on the event-dispatch thread
 * with a fresh demo galaxy.
 *
 * <p>In a headless environment (no display — e.g. CI) it cannot open a window,
 * so it instead runs a short console simulation as a smoke check and exits,
 * proving the model + engine work end-to-end without a display.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            runHeadlessSmoke();
            return;
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to the default look and feel.
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainWindow window = new MainWindow(DemoGalaxy.build());
                window.setVisible(true);
            }
        });
    }

    private static void runHeadlessSmoke() {
        System.out.println("[headless] No display available — running a 10-year console simulation.");
        Galaxy galaxy = DemoGalaxy.build();
        TurnEngine engine = new TurnEngine(galaxy);
        for (int i = 0; i < 10; i++) {
            engine.generateTurn();
        }
        int planets = galaxy.planetsOf(galaxy.player(DemoGalaxy.HUMAN_ID)).size();
        System.out.println("[headless] Reached year " + galaxy.year()
                + "; You own " + planets + " planet(s), "
                + galaxy.fleets().size() + " fleet(s) in play.");
        System.out.println("[headless] OK — launch on a desktop to play the Swing UI.");
    }
}
