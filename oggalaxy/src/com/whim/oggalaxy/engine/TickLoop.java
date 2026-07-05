package com.whim.oggalaxy.engine;

/**
 * Background clock. Applies one simulation tick at a time and sleeps so that roughly
 * {@code engine.getSpeed()} ticks are applied per real second. All mutation happens inside
 * {@link GameEngine#advance(int)} (which is synchronized), so the loop never touches state
 * directly and never blocks the EDT.
 */
final class TickLoop {

    private final GameEngine engine;
    private final Thread thread;
    private volatile boolean running = true;

    TickLoop(GameEngine engine) {
        this.engine = engine;
        this.thread = new Thread(new Runnable() {
            @Override public void run() { loop(); }
        }, "oggalaxy-tick-loop");
        this.thread.setDaemon(true);
    }

    void start() {
        thread.start();
    }

    void shutdown() {
        running = false;
        thread.interrupt();
    }

    private void loop() {
        while (running && engine.isClockRunning()) {
            try {
                engine.advance(1);
                int spd = Math.max(1, engine.getSpeed());
                Thread.sleep(Math.max(1L, 1000L / spd));
            } catch (InterruptedException ie) {
                return;
            } catch (RuntimeException ex) {
                // never let a tick error kill the loop silently mid-game
                try { Thread.sleep(50L); } catch (InterruptedException ignored) { return; }
            }
        }
    }
}
