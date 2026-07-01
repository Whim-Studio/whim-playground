package com.whim.colony.engine;

import com.whim.colony.ColonyState;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A fixed-cadence driver that ticks a {@link Simulation} on a background
 * {@link javax.swing.Timer}. The default cadence is {@value #DEFAULT_TICK_MS} ms
 * (~{@value #DEFAULT_TPS} ticks/second), decoupled from the UI repaint rate.
 *
 * <p>The clock honours {@link ColonyState#isPaused()}: while paused the timer
 * keeps running but skips the tick, so pausing/resuming is instant and lossless.
 * A Swing {@code Timer} fires on the Event Dispatch Thread, which keeps mutation
 * of the shared {@link ColonyState} single-threaded relative to the UI's reads —
 * no locking required.
 *
 * <p>All simulation logic lives in {@link Simulation}; this class only schedules
 * calls to it, so the engine itself stays free of any UI dependency.
 */
public final class SimulationClock {

    /** Default milliseconds between ticks. */
    public static final int DEFAULT_TICK_MS = 100;
    /** Nominal ticks per second at the default cadence. */
    public static final int DEFAULT_TPS = 1000 / DEFAULT_TICK_MS;

    private final Simulation simulation;
    private final ColonyState state;
    private final Timer timer;

    public SimulationClock(Simulation simulation, ColonyState state) {
        this(simulation, state, DEFAULT_TICK_MS);
    }

    public SimulationClock(Simulation simulation, ColonyState state, int tickMs) {
        this.simulation = simulation;
        this.state = state;
        this.timer = new Timer(Math.max(1, tickMs), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!SimulationClock.this.state.isPaused()) {
                    SimulationClock.this.simulation.tick(SimulationClock.this.state);
                }
            }
        });
        this.timer.setCoalesce(true);
    }

    /** Start (or restart) the automatic tick cadence. */
    public void start() {
        timer.start();
    }

    /** Stop the automatic tick cadence entirely. The state is left untouched. */
    public void stop() {
        timer.stop();
    }

    /** @return true if the timer is currently running (independent of pause). */
    public boolean isRunning() {
        return timer.isRunning();
    }

    /** Change the tick cadence in milliseconds, effective immediately. */
    public void setTickMs(int tickMs) {
        timer.setDelay(Math.max(1, tickMs));
    }

    /**
     * Force a single tick right now, regardless of pause state. Useful for a UI
     * "step" control while the simulation is paused.
     */
    public void step() {
        simulation.tick(state);
    }
}
