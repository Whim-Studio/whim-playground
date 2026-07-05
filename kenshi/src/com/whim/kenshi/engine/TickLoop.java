package com.whim.kenshi.engine;

import com.whim.kenshi.api.Config;

/**
 * Background driver that invokes a tick callback at a fixed {@link Config#TICK_HZ}
 * cadence on its own daemon thread, fully decoupled from Swing. Pause and game
 * speed are honoured inside the callback (the loop always fires at TICK_HZ; the
 * engine decides how many simulation sub-steps to run). Idempotent start/stop.
 */
final class TickLoop {

    /** The per-tick work, supplied by the engine. */
    interface Tick {
        void run();
    }

    private final Tick tick;
    private final long periodNanos;
    private volatile boolean running;
    private Thread thread;

    TickLoop(Tick tick) {
        this.tick = tick;
        this.periodNanos = 1_000_000_000L / Config.TICK_HZ;
    }

    synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(new Runnable() {
            public void run() {
                loop();
            }
        }, "kenshi-tick");
        thread.setDaemon(true);
        thread.start();
    }

    synchronized void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    boolean isRunning() {
        return running;
    }

    private void loop() {
        long next = System.nanoTime();
        while (running) {
            try {
                tick.run();
            } catch (Throwable t) {
                // Never let one bad tick kill the loop; report and continue.
                System.err.println("[kenshi-tick] tick failed: " + t);
                t.printStackTrace();
            }
            next += periodNanos;
            long sleep = next - System.nanoTime();
            if (sleep < 0) {
                // We're behind; resync rather than spiralling.
                next = System.nanoTime();
                sleep = 0;
            }
            if (sleep > 0) {
                long ms = sleep / 1_000_000L;
                int ns = (int) (sleep % 1_000_000L);
                try {
                    Thread.sleep(ms, ns);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}
