package com.whim.settlers.engine;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;

/**
 * Fixed-timestep game loop with active rendering.
 *
 * <p>Simulation advances in fixed {@value #TICK_MS}-ms steps via an accumulator,
 * so behaviour is independent of frame rate; rendering happens as fast as the
 * loop allows and is throttled by a sleep to a target FPS. We render actively
 * through a {@link BufferStrategy} on an AWT {@link Canvas} rather than relying
 * on Swing's passive {@code repaint()} — the standard approach for a smooth game
 * loop and what the project spec calls for.
 */
public final class GameLoop implements Runnable {

    /** Fixed simulation step. 60 updates/sec. */
    private static final double TICK_MS = 1000.0 / 60.0;
    private static final long   TARGET_FRAME_NS = 1_000_000_000L / 120; // cap render rate

    private final Canvas canvas;
    private final World world;
    private final InputHandler input;
    private final Renderer renderer = new Renderer();

    private volatile boolean running;
    private Thread thread;
    private double fps;

    public GameLoop(Canvas canvas, World world, InputHandler input) {
        this.canvas = canvas;
        this.world = world;
        this.input = input;
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(this, "settlers-game-loop");
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        if (thread != null) {
            try { thread.join(1000); } catch (InterruptedException ignored) { }
        }
    }

    @Override
    public void run() {
        // Create the double-buffer once the canvas is displayable.
        while (running && !canvas.isDisplayable()) {
            sleep(10);
        }
        if (!running) return;
        canvas.createBufferStrategy(2);
        BufferStrategy bs = canvas.getBufferStrategy();

        long prev = System.nanoTime();
        double accumulatorMs = 0;
        long lastFpsSample = prev;
        int frames = 0;

        while (running) {
            long now = System.nanoTime();
            double elapsedMs = (now - prev) / 1_000_000.0;
            prev = now;
            // Guard against huge steps (e.g. after the window was dragged/paused).
            if (elapsedMs > 250) elapsedMs = 250;
            accumulatorMs += elapsedMs;

            while (accumulatorMs >= TICK_MS) {
                double dt = TICK_MS / 1000.0;
                input.applyContinuous(dt);
                world.update(dt);
                accumulatorMs -= TICK_MS;
            }

            renderFrame(bs);

            frames++;
            if (now - lastFpsSample >= 1_000_000_000L) {
                fps = frames * 1_000_000_000.0 / (now - lastFpsSample);
                frames = 0;
                lastFpsSample = now;
            }

            long frameNs = System.nanoTime() - now;
            long sleepNs = TARGET_FRAME_NS - frameNs;
            if (sleepNs > 0) sleep(sleepNs / 1_000_000L, (int) (sleepNs % 1_000_000L));
        }
    }

    private void renderFrame(BufferStrategy bs) {
        Dimension size = canvas.getSize();
        world.camera().setViewport(size.width, size.height);
        do {
            do {
                Graphics2D g = (Graphics2D) bs.getDrawGraphics();
                try {
                    renderer.render(g, world, input, fps);
                } finally {
                    g.dispose();
                }
            } while (bs.contentsRestored());
            bs.show();
        } while (bs.contentsLost());
    }

    private static void sleep(long ms) { sleep(ms, 0); }

    private static void sleep(long ms, int ns) {
        try { Thread.sleep(Math.max(0, ms), Math.max(0, ns)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
