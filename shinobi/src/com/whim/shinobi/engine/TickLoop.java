package com.whim.shinobi.engine;

import com.whim.shinobi.api.Config;

/**
 * A background thread that advances the simulation at exactly {@link Config#TICK_HZ}
 * using {@link System#nanoTime()} pacing, fully decoupled from Swing. It calls a
 * single {@link Runnable} callback once per tick.
 *
 * Lifecycle: {@link #start()} launches the thread; {@link #stop()} joins it cleanly;
 * {@link #setPaused(boolean)} freezes ticking without tearing the thread down.
 */
final class TickLoop {
    private static final long NANOS_PER_TICK = 1_000_000_000L / Config.TICK_HZ;

    private final Runnable onTick;
    private volatile boolean running = false;
    private volatile boolean paused = false;
    private Thread thread;

    TickLoop(Runnable onTick) {
        this.onTick = onTick;
    }

    synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(new Runnable() {
            @Override public void run() { loop(); }
        }, "shinobi-tick");
        thread.setDaemon(true);
        thread.start();
    }

    synchronized void stop() {
        if (!running) return;
        running = false;
        Thread t = thread;
        thread = null;
        if (t != null) {
            t.interrupt();
            try {
                t.join(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    void setPaused(boolean p) { this.paused = p; }
    boolean isPaused() { return paused; }
    boolean isRunning() { return running; }

    private void loop() {
        long next = System.nanoTime();
        while (running) {
            if (!paused) {
                try {
                    onTick.run();
                } catch (RuntimeException ex) {
                    // Never let a bad tick kill the loop; report and continue.
                    System.err.println("[TickLoop] tick error: " + ex);
                }
            }
            next += NANOS_PER_TICK;
            long sleep = next - System.nanoTime();
            if (sleep > 0) {
                long ms = sleep / 1_000_000L;
                int ns = (int) (sleep % 1_000_000L);
                try {
                    Thread.sleep(ms, ns);
                } catch (InterruptedException ie) {
                    if (!running) break;
                }
            } else if (sleep < -NANOS_PER_TICK * 4) {
                // Fell far behind (e.g. after a pause/GC stall): resync to now.
                next = System.nanoTime();
            }
        }
    }
}
