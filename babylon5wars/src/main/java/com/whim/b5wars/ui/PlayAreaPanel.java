package com.whim.b5wars.ui;

import com.whim.b5wars.engine.GameEvent;
import com.whim.b5wars.model.Facing;
import com.whim.b5wars.model.Hex;
import com.whim.b5wars.model.Scenario;
import com.whim.b5wars.model.Ship;
import com.whim.b5wars.model.Weapon;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Random;

import javax.swing.JPanel;

/**
 * The tactical map: procedural starfield, hex field, faction-colored ship triangles (nose = facing),
 * movement vectors, and — for the selected ship's selected weapon — a firing-arc overlay plus range
 * rings. Click a hex to select the ship on it (or just the hex). All shapes are original artwork.
 */
public final class PlayAreaPanel extends JPanel implements GameListener {

    private final GameController controller;
    private final Scenario scenario;

    // Stable procedural starfield (generated once, redrawn each paint).
    private int[] starX;
    private int[] starY;
    private float[] starMag;
    private int starW = -1;
    private int starH = -1;

    private HexView view;

    public PlayAreaPanel(GameController controller) {
        this.controller = controller;
        this.scenario = controller.state().getScenario();
        setBackground(UiTheme.SPACE_BOTTOM);
        setPreferredSize(new Dimension(720, 720));
        controller.addListener(this);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                onClick(e.getX(), e.getY());
            }
        });
    }

    @Override
    public void gameChanged() {
        repaint();
    }

    @Override
    public void logEvents(List<GameEvent> events) {
        // The map does not itself show the log.
    }

    private void onClick(int px, int py) {
        if (view == null) {
            return;
        }
        // Prefer selecting a ship near the click.
        Ship nearest = null;
        double best = Double.MAX_VALUE;
        for (Ship s : controller.state().getShips()) {
            Point2D.Double c = view.center(s.getPos());
            double d = Math.hypot(px - c.x, py - c.y);
            if (d < best) {
                best = d;
                nearest = s;
            }
        }
        int[] hex = view.pixelToHex(px, py);
        controller.selectHex(hex[0], hex[1]);
        if (nearest != null && best <= view.size() * 1.1) {
            controller.selectShip(nearest);
        }
    }

    private void ensureStarfield(int w, int h) {
        if (w == starW && h == starH && starX != null) {
            return;
        }
        starW = w;
        starH = h;
        int count = Math.max(120, (w * h) / 1600);
        starX = new int[count];
        starY = new int[count];
        starMag = new float[count];
        Random rng = new Random(0xB5B5B5L); // fixed → stable field
        for (int i = 0; i < count; i++) {
            starX[i] = rng.nextInt(Math.max(1, w));
            starY[i] = rng.nextInt(Math.max(1, h));
            starMag[i] = 0.25f + rng.nextFloat() * 0.75f;
        }
    }

    private void recomputeView(int w, int h) {
        int mw = Math.max(1, scenario.getMapWidth());
        int mh = Math.max(1, scenario.getMapHeight());
        double wUnits = 1.5 * (mw - 1) + 2.0;
        double hUnits = HexView.SQRT3 * ((mh - 1) + (mw - 1) / 2.0) + 2.0;
        double size = Math.min((w - 8) / wUnits, (h - 8) / hUnits);
        if (size < 4) {
            size = 4;
        }
        double originX = size;      // one-radius margin
        double originY = size;
        this.view = new HexView(size, originX, originY);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();

        // Background gradient + starfield.
        g.setPaint(new GradientPaint(0, 0, UiTheme.SPACE_TOP, 0, h, UiTheme.SPACE_BOTTOM));
        g.fillRect(0, 0, w, h);
        ensureStarfield(w, h);
        for (int i = 0; i < starX.length; i++) {
            float m = starMag[i];
            g.setColor(new Color(m, m, Math.min(1f, m + 0.1f)));
            int r = m > 0.85f ? 2 : 1;
            g.fillOval(starX[i], starY[i], r, r);
        }

        recomputeView(w, h);
        drawHexGrid(g);
        drawWeaponOverlay(g);
        drawVectors(g);
        drawShips(g);
        drawSelection(g);

        g.dispose();
    }

    private void drawHexGrid(Graphics2D g) {
        g.setStroke(new BasicStroke(1f));
        int mw = scenario.getMapWidth();
        int mh = scenario.getMapHeight();
        for (int q = 0; q < mw; q++) {
            for (int r = 0; r < mh; r++) {
                Polygon p = view.hexPolygon(q, r);
                g.setColor((q + r) % 2 == 0 ? UiTheme.HEX_LINE : UiTheme.HEX_LINE_DIM);
                g.drawPolygon(p);
            }
        }
    }

    private void drawWeaponOverlay(Graphics2D g) {
        Ship s = controller.selectedShip();
        if (s == null || s.isDestroyed()) {
            return;
        }
        List<Weapon> weapons = s.getType().getWeapons();
        int wi = controller.selectedWeaponIndex();
        if (wi < 0 || wi >= weapons.size()) {
            return;
        }
        Weapon weapon = weapons.get(wi);
        Point2D.Double c = view.center(s.getPos());
        int[] brackets = weapon.getRangeBrackets();
        double pitch = view.hexPitch();
        int maxRange = brackets.length == 0 ? 6 : brackets[brackets.length - 1];

        // Firing-arc wedges (one 60° sector per absolute facing in the arc).
        g.setColor(UiTheme.ARC_FILL);
        double rad = maxRange * pitch;
        for (Facing rel : weapon.getArc().facings()) {
            Facing abs = Facing.values()[(rel.index() + s.getFacing().index()) % 6];
            fillWedge(g, c.x, c.y, rad, HexView.facingAngle(abs), Math.toRadians(30));
        }

        // Range rings at each bracket.
        g.setColor(UiTheme.RANGE_RING);
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                1f, new float[] {4f, 4f}, 0f));
        for (int i = 0; i < brackets.length; i++) {
            double rr = brackets[i] * pitch;
            g.drawOval((int) (c.x - rr), (int) (c.y - rr), (int) (2 * rr), (int) (2 * rr));
        }
        g.setStroke(new BasicStroke(1f));
    }

    private void fillWedge(Graphics2D g, double cx, double cy, double radius,
                           double centerAngle, double halfSpan) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(cx, cy);
        int steps = 10;
        for (int i = 0; i <= steps; i++) {
            double a = centerAngle - halfSpan + (2 * halfSpan) * i / steps;
            path.lineTo(cx + radius * Math.cos(a), cy + radius * Math.sin(a));
        }
        path.closePath();
        g.fill(path);
    }

    private void drawVectors(Graphics2D g) {
        double pitch = view.hexPitch();
        g.setStroke(new BasicStroke(2f));
        for (Ship s : controller.state().getShips()) {
            if (s.isDestroyed() || s.getSpeed() <= 0) {
                continue;
            }
            Point2D.Double c = view.center(s.getPos());
            double[] u = HexView.facingUnit(s.getFacing());
            double len = s.getSpeed() * pitch;
            double ex = c.x + u[0] * len;
            double ey = c.y + u[1] * len;
            g.setColor(UiTheme.VECTOR);
            g.drawLine((int) c.x, (int) c.y, (int) ex, (int) ey);
            // Arrowhead.
            double ang = Math.atan2(u[1], u[0]);
            double ah = 8;
            g.drawLine((int) ex, (int) ey,
                    (int) (ex - ah * Math.cos(ang - Math.PI / 7)),
                    (int) (ey - ah * Math.sin(ang - Math.PI / 7)));
            g.drawLine((int) ex, (int) ey,
                    (int) (ex - ah * Math.cos(ang + Math.PI / 7)),
                    (int) (ey - ah * Math.sin(ang + Math.PI / 7)));
        }
    }

    private void drawShips(Graphics2D g) {
        double size = view.size();
        for (Ship s : controller.state().getShips()) {
            Point2D.Double c = view.center(s.getPos());
            double ang = HexView.facingAngle(s.getFacing());
            double nose = size * 0.95;
            double back = size * 0.75;
            double spread = Math.toRadians(140);
            Polygon tri = new Polygon();
            tri.addPoint((int) (c.x + nose * Math.cos(ang)), (int) (c.y + nose * Math.sin(ang)));
            tri.addPoint((int) (c.x + back * Math.cos(ang + spread)),
                    (int) (c.y + back * Math.sin(ang + spread)));
            tri.addPoint((int) (c.x + back * Math.cos(ang - spread)),
                    (int) (c.y + back * Math.sin(ang - spread)));

            Color fill = UiTheme.colorForShip(s);
            if (s.isDestroyed()) {
                fill = new Color(70, 70, 78);
            }
            g.setColor(fill);
            g.fillPolygon(tri);
            g.setStroke(new BasicStroke(1.6f));
            g.setColor(UiTheme.outlineForSide(s.getSide()));
            g.drawPolygon(tri);

            if (s.isDestroyed()) {
                g.setColor(new Color(230, 90, 90));
                g.drawLine((int) (c.x - size * 0.6), (int) (c.y - size * 0.6),
                        (int) (c.x + size * 0.6), (int) (c.y + size * 0.6));
                g.drawLine((int) (c.x - size * 0.6), (int) (c.y + size * 0.6),
                        (int) (c.x + size * 0.6), (int) (c.y - size * 0.6));
            }
        }
    }

    private void drawSelection(Graphics2D g) {
        int[] hex = controller.selectedHex();
        if (hex != null) {
            g.setColor(new Color(255, 232, 120, 60));
            g.fill(view.hexPolygon(hex[0], hex[1]));
        }
        Ship s = controller.selectedShip();
        if (s != null) {
            Point2D.Double c = view.center(s.getPos());
            g.setColor(UiTheme.SELECT);
            g.setStroke(new BasicStroke(2f));
            double rr = view.size() * 1.15;
            g.drawOval((int) (c.x - rr), (int) (c.y - rr), (int) (2 * rr), (int) (2 * rr));
        }
    }
}
