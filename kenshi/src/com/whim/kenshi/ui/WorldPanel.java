package com.whim.kenshi.ui;

import com.whim.kenshi.api.Config;
import com.whim.kenshi.api.Enums;
import com.whim.kenshi.api.GameController;
import com.whim.kenshi.api.Views;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * The main world viewport. A {@link javax.swing.Timer} drives repaint at ~40fps;
 * each tick it polls {@link GameController#state()} once, hands the coherent
 * snapshot to any registered sink (HUD / BodyChart) and repaints. Draws terrain,
 * nodes and characters through the {@link Renderer}, plus the live drag-select
 * rectangle and transient order feedback.
 */
public final class WorldPanel extends JPanel {

    /** Receives the once-per-frame snapshot so sibling panels read the same data. */
    public interface FrameSink { void onFrame(Views.GameStateView state); }

    private final GameController controller;
    private final Camera camera;
    private final Renderer renderer;
    private final Timer timer;

    private volatile Views.GameStateView state;
    private FrameSink sink;

    // Live drag-select rectangle in SCREEN coordinates (null when not dragging).
    private Rectangle dragRect;
    // Transient order-feedback markers.
    private final List<OrderFlash> flashes = new ArrayList<OrderFlash>();

    public WorldPanel(GameController controller, Camera camera, Renderer renderer) {
        this.controller = controller;
        this.camera = camera;
        this.renderer = renderer;
        setPreferredSize(new Dimension(Config.VIEW_W, Config.VIEW_H));
        setBackground(new Color(18, 16, 15));
        setFocusable(true);

        this.timer = new Timer(25, new ActionListener() {
            public void actionPerformed(ActionEvent e) { tick(); }
        });
    }

    public void setFrameSink(FrameSink sink) { this.sink = sink; }
    public Camera camera() { return camera; }
    public Views.GameStateView currentState() { return state; }

    public void startAnimating() { timer.start(); }
    public void stopAnimating() { timer.stop(); }

    private void tick() {
        camera.setViewport(getWidth(), getHeight());
        Views.GameStateView s = controller.state();
        this.state = s;
        if (sink != null) sink.onFrame(s);
        // Age out order-feedback markers.
        for (int i = flashes.size() - 1; i >= 0; i--) {
            if (flashes.get(i).age()) flashes.remove(i);
        }
        repaint();
    }

    // ---- input hooks -----------------------------------------------------
    public void setDragRect(Rectangle r) { this.dragRect = r; }

    public void flashOrder(double worldX, double worldY, Enums.OrderType type) {
        flashes.add(new OrderFlash(worldX, worldY, type));
    }

    // ---- painting --------------------------------------------------------
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        Views.GameStateView s = state;
        if (s == null) {
            g.setColor(Palette.HUD_TEXT_DIM);
            g.drawString("Loading world…", 20, 30);
            return;
        }

        drawTerrainAndBounds(g, s);

        List<Views.NodeView> nodes = s.nodes();
        for (int i = 0; i < nodes.size(); i++) {
            renderer.drawNode(g, nodes.get(i), camera);
        }

        drawOrderFeedback(g, s);

        // Dead first, then living, so living characters render on top.
        List<Views.CharacterView> chars = s.characters();
        for (int i = 0; i < chars.size(); i++) {
            if (chars.get(i).moveState() == Enums.MoveState.DEAD) {
                renderer.drawCharacter(g, chars.get(i), camera);
            }
        }
        for (int i = 0; i < chars.size(); i++) {
            if (chars.get(i).moveState() != Enums.MoveState.DEAD) {
                renderer.drawCharacter(g, chars.get(i), camera);
            }
        }

        drawFlashes(g);
        drawDragRect(g);
        drawPausedBanner(g, s);
    }

    private void drawTerrainAndBounds(Graphics2D g, Views.GameStateView s) {
        renderer.drawTerrain(g, s.map(), camera);
        // World edge outline.
        double x0 = camera.toScreenX(0);
        double y0 = camera.toScreenY(0);
        double x1 = camera.toScreenX(Config.WORLD_SIZE);
        double y1 = camera.toScreenY(Config.WORLD_SIZE);
        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(0, 0, 0, 120));
        g.draw(new Rectangle2D.Double(x0, y0, x1 - x0, y1 - y0));
    }

    private void drawOrderFeedback(Graphics2D g, Views.GameStateView s) {
        List<Views.CharacterView> chars = s.characters();
        for (int i = 0; i < chars.size(); i++) {
            Views.CharacterView ch = chars.get(i);
            if (!ch.selected()) continue;
            Enums.OrderType ot = ch.orderType();
            if (ot == null || ot == Enums.OrderType.NONE) continue;
            double sx = camera.toScreenX(ch.x());
            double sy = camera.toScreenY(ch.y());
            if (ot == Enums.OrderType.ATTACK && ch.targetId() != null) {
                Views.CharacterView t = find(chars, ch.targetId());
                if (t != null) {
                    g.setStroke(dashed());
                    g.setColor(Palette.ORDER_ATTACK);
                    g.draw(new Line2D.Double(sx, sy, camera.toScreenX(t.x()), camera.toScreenY(t.y())));
                }
            }
        }
    }

    private void drawFlashes(Graphics2D g) {
        for (int i = 0; i < flashes.size(); i++) {
            OrderFlash f = flashes.get(i);
            double sx = camera.toScreenX(f.x);
            double sy = camera.toScreenY(f.y);
            float alpha = Math.max(0f, Math.min(1f, f.ttl / (float) OrderFlash.LIFE));
            Color c = colorFor(f.type);
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (alpha * 220)));
            g.setStroke(new BasicStroke(2f));
            double r = 6 + (OrderFlash.LIFE - f.ttl) * 0.7;
            g.draw(new Ellipse2D.Double(sx - r, sy - r, r * 2, r * 2));
            if (f.type == Enums.OrderType.MOVE) {
                g.draw(new Line2D.Double(sx - 4, sy, sx + 4, sy));
                g.draw(new Line2D.Double(sx, sy - 4, sx, sy + 4));
            }
        }
    }

    private void drawDragRect(Graphics2D g) {
        Rectangle r = dragRect;
        if (r == null || (r.width == 0 && r.height == 0)) return;
        g.setColor(Palette.SELECT_RECT);
        g.fill(r);
        g.setStroke(new BasicStroke(1.4f));
        g.setColor(Palette.SELECT_RECT_EDGE);
        g.draw(r);
    }

    private void drawPausedBanner(Graphics2D g, Views.GameStateView s) {
        if (s.phase() != Enums.Phase.PAUSED) return;
        g.setColor(Palette.HUD_ACCENT);
        g.drawString("|| PAUSED", getWidth() - 74, 18);
    }

    private static Views.CharacterView find(List<Views.CharacterView> list, String id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(id)) return list.get(i);
        }
        return null;
    }

    private static Color colorFor(Enums.OrderType t) {
        if (t == Enums.OrderType.ATTACK) return Palette.ORDER_ATTACK;
        if (t == Enums.OrderType.INTERACT) return Palette.ORDER_INTERACT;
        return Palette.ORDER_MOVE;
    }

    private static BasicStroke dashed() {
        return new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                8f, new float[]{6f, 5f}, 0f);
    }

    /** A short-lived visual pulse marking where the player just issued an order. */
    private static final class OrderFlash {
        static final int LIFE = 22;
        final double x, y;
        final Enums.OrderType type;
        int ttl = LIFE;
        OrderFlash(double x, double y, Enums.OrderType type) { this.x = x; this.y = y; this.type = type; }
        boolean age() { return --ttl <= 0; }
    }
}
