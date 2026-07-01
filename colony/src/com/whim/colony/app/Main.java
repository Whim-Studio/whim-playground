package com.whim.colony.app;

import java.util.Random;

import javax.swing.SwingUtilities;

import com.whim.colony.ColonyState;
import com.whim.colony.engine.MapGenerator;
import com.whim.colony.engine.Simulation;
import com.whim.colony.engine.SimulationClock;
import com.whim.colony.ui.GameFrame;

/**
 * Entry point for the Colony simulation.
 *
 * <p>Wires the three layers together:
 * <ul>
 *   <li>Task 1 (domain) provides the shared {@link ColonyState}.</li>
 *   <li>Task 2 (engine) builds the world ({@link MapGenerator}) and drives it
 *       off-thread-of-rendering via the {@link SimulationClock} at a fixed
 *       tick cadence, independent of the UI frame rate.</li>
 *   <li>Task 3 (UI) reads that same {@link ColonyState} and repaints on its own
 *       30fps view timer; it never runs simulation logic.</li>
 * </ul>
 *
 * <p>The simulation clock and the UI refresh timer both fire on the Swing EDT,
 * so state mutation (engine) and state reads (UI) stay single-threaded without
 * explicit locking, while remaining logically decoupled: pausing the sim
 * (SPACE) stops ticks but the view keeps rendering the frozen world.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        // Deterministic seed so the demo world is reproducible; pass an arg to vary it.
        final long seed = args.length > 0 ? parseSeed(args[0]) : 20260701L;

        // Build the world and the simulation engine (Task 2).
        final ColonyState state = new MapGenerator(new Random(seed)).generate();
        final Simulation simulation = new Simulation(new Random(seed));
        final SimulationClock clock = new SimulationClock(simulation, state);

        state.addMessage("Colony founded. Seed " + seed + ".");

        // Build the UI (Task 3) on the EDT, then start both timers.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                GameFrame frame = new GameFrame(state);
                frame.setVisible(true);
                frame.startRefresh(); // ~30fps view repaint (rendering only)
                clock.start();        // ~10 TPS simulation tick (respects state.isPaused())
            }
        });
    }

    private static long parseSeed(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            // Fall back to a hash so any string still yields a stable seed.
            return raw.hashCode();
        }
    }
}
