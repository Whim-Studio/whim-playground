package com.whim.shinobi.ui;

import com.whim.shinobi.api.Config;
import com.whim.shinobi.api.GameController;
import com.whim.shinobi.api.Views;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The playfield + HUD surface. Holds a {@link GameController}, a ~60fps Swing
 * {@link Timer} that polls {@code controller.state()} and repaints, and wires an
 * {@link InputHandler}. All painting happens on the EDT via the timer; the timer
 * only reads a snapshot, never blocks on the engine. Double-buffered by JPanel.
 */
public final class GamePanel extends JPanel {

    private final GameController controller;
    private final Renderer renderer = new Renderer();
    private final Hud hud = new Hud();
    private final Camera camera = new Camera();
    private final Timer timer;

    public GamePanel(GameController controller) {
        this.controller = controller;
        setPreferredSize(new Dimension(Config.VIEW_W, Hud.TOTAL_H));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);

        InputHandler input = new InputHandler(controller);
        addKeyListener(input);

        // ~60 fps repaint cadence, decoupled from the engine's tick thread.
        int periodMs = Math.max(1, 1000 / Config.TICK_HZ);
        timer = new Timer(periodMs, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { repaint(); }
        });
    }

    public void startRenderLoop() {
        timer.start();
        requestFocusInWindow();
    }

    public void stopRenderLoop() {
        timer.stop();
    }

    public GameController controller() { return controller; }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;

        Views.GameStateView state = controller.state();
        if (state == null) return;
        camera.update(state);

        // Top HUD bar
        hud.drawTop(g, state);

        // Playfield: clip + translate so world (0,0..VIEW) sits below the top bar.
        Shape oldClip = g.getClip();
        g.translate(0, Hud.TOP_H);
        g.setClip(0, 0, Config.VIEW_W, Config.VIEW_H);
        renderer.render(g, state, camera);
        hud.drawPhaseOverlay(g, state);
        g.setClip(oldClip);
        g.translate(0, -Hud.TOP_H);

        // Bottom HUD bar
        g.translate(0, Hud.TOP_H + Config.VIEW_H);
        hud.drawBottom(g, state);
        g.translate(0, -(Hud.TOP_H + Config.VIEW_H));
    }
}
