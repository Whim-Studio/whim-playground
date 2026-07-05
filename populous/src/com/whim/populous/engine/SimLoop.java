package com.whim.populous.engine;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * A background simulation thread that fires a fixed-timestep callback at a
 * target rate (~30 ticks/sec), fully decoupled from Swing. Supports pause /
 * resume and a clean, join-based shutdown. Built on {@code java.util.concurrent}
 * primitives — deliberately NOT a {@code javax.swing.Timer}, so the sim never
 * runs on the EDT.
 *
 * The loop accumulates elapsed real time and issues catch-up ticks so the
 * simulation advances at a steady wall-clock rate even if a tick runs long,
 * while capping catch-up to avoid a spiral of death.
 */
final class SimLoop {

    /** Target simulation rate. */
    static final int TICKS_PER_SECOND = 30;
    private static final long TICK_NANOS = 1_000_000_000L / TICKS_PER_SECOND;
    private static final int MAX_CATCHUP = 5;

    private final Runnable tick;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);

    private volatile Thread thread;

    SimLoop(Runnable tick) {
        this.tick = tick;
    }

    boolean isRunning() {
        return running.get();
    }

    boolean isPaused() {
        return paused.get();
    }

    /** Start (or resume) the loop. Idempotent. */
    synchronized void start() {
        paused.set(false);
        if (running.get()) {
            return;
        }
        running.set(true);
        thread = new Thread(new Runnable() {
            public void run() {
                loop();
            }
        }, "populous-sim");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Ask the loop to exit WITHOUT joining — safe to call from inside the tick
     * callback (i.e. on the sim thread itself), where {@link #stop} would
     * deadlock trying to join its own thread.
     */
    void requestStop() {
        running.set(false);
    }

    /** Pause ticking without tearing down the thread. */
    void pause() {
        paused.set(true);
    }

    void resume() {
        paused.set(true);
        paused.set(false);
    }

    /** Stop the loop and block until the thread has fully exited. */
    synchronized void stop() {
        running.set(false);
        Thread t = thread;
        if (t != null) {
            LockSupport.unpark(t);
            try {
                t.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            thread = null;
        }
    }

    private void loop() {
        long previous = System.nanoTime();
        long accumulator = 0L;
        while (running.get()) {
            long now = System.nanoTime();
            long elapsed = now - previous;
            previous = now;

            if (paused.get()) {
                // Idle politely; discard accumulated time so we don't burst on resume.
                accumulator = 0L;
                LockSupport.parkNanos(TICK_NANOS);
                previous = System.nanoTime();
                continue;
            }

            accumulator += elapsed;
            int steps = 0;
            while (accumulator >= TICK_NANOS && steps < MAX_CATCHUP && running.get()) {
                safeTick();
                accumulator -= TICK_NANOS;
                steps++;
            }
            if (accumulator > TICK_NANOS * MAX_CATCHUP) {
                accumulator = 0L; // fell too far behind; drop the debt
            }

            long sleep = TICK_NANOS - (System.nanoTime() - now);
            if (sleep > 0) {
                LockSupport.parkNanos(sleep);
            }
        }
    }

    private void safeTick() {
        try {
            tick.run();
        } catch (RuntimeException ex) {
            // A single bad tick must not kill the whole simulation thread.
            System.err.println("[populous-sim] tick error: " + ex);
        }
    }
}
