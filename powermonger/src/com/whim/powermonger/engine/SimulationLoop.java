package com.whim.powermonger.engine;

/**
 * Fixed-timestep driver. Runs a background <b>daemon</b> thread that invokes a step
 * callback ~{@code ticksPerSecond} times a second. The callback (the engine's
 * {@code stepOnce}) does all world mutation under the engine lock, so the loop
 * thread never tears the snapshot the EDT reads.
 */
public final class SimulationLoop {

    private final Runnable step;
    private final long tickNanos;

    private volatile boolean running;
    private Thread thread;

    public SimulationLoop(Runnable step, int ticksPerSecond) {
        if (step == null) {
            throw new IllegalArgumentException("step callback required");
        }
        this.step = step;
        int tps = ticksPerSecond <= 0 ? 20 : ticksPerSecond;
        this.tickNanos = 1_000_000_000L / tps;
    }

    /** Start the daemon thread. Idempotent. */
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(new Runnable() {
            @Override public void run() {
                loop();
            }
        }, "powermonger-sim");
        thread.setDaemon(true);
        thread.start();
    }

    /** Stop the daemon thread. Idempotent. */
    public synchronized void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    public boolean isRunning() {
        return running;
    }

    private void loop() {
        long next = System.nanoTime();
        while (running) {
            try {
                step.run();
            } catch (RuntimeException ex) {
                // A subsystem fault must not kill the loop silently.
                System.err.println("[sim] tick error: " + ex);
            }
            next += tickNanos;
            long sleep = next - System.nanoTime();
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep / 1_000_000L, (int) (sleep % 1_000_000L));
                } catch (InterruptedException ie) {
                    if (!running) {
                        break;
                    }
                }
            } else {
                // Fell behind; resync rather than spiral.
                next = System.nanoTime();
            }
        }
    }
}
