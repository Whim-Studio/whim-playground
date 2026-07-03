package com.whim.powermonger.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.List;

import javax.swing.JPanel;

import com.whim.powermonger.api.Enums.TerrainType;
import com.whim.powermonger.api.GameController;
import com.whim.powermonger.api.Views.CaptainView;
import com.whim.powermonger.api.Views.GameStateView;
import com.whim.powermonger.api.Views.PigeonView;
import com.whim.powermonger.api.Views.TileView;
import com.whim.powermonger.api.Views.TownView;
import com.whim.powermonger.api.Views.TownspersonView;

/**
 * Central 2.5D pseudo-isometric view. Renders the world snapshot via
 * {@link IsoRenderer} + {@link SpriteFactory}, draws a destination line from the
 * selected captain to its target, and translates mouse clicks into controller
 * actions (select captain / set destination).
 */
public final class MapPanel extends JPanel {

    private final GameController controller;
    private final IsoRenderer iso = new IsoRenderer();
    private double animPhase = 0;

    public MapPanel(GameController controller) {
        this.controller = controller;
        setBackground(new Color(18, 22, 30));

        // Centre the camera on the map middle once we know its size.
        GameStateView st = controller.state();
        iso.centerOn(st.mapWidth() / 2.0, st.mapHeight() / 2.0);

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { handleClick(e); }
        };
        addMouseListener(mouse);
    }

    /** Advance the animation clock; called by the frame's Swing timer. */
    public void tickAnimation() {
        animPhase += 0.2;
    }

    /** Recenter the isometric camera (used by the minimap). */
    public void centerOn(double tileX, double tileY) {
        iso.centerOn(tileX, tileY);
        repaint();
    }

    public IsoRenderer renderer() { return iso; }

    // ---- input ----------------------------------------------------------

    private void handleClick(MouseEvent e) {
        GameStateView st = controller.state();
        // 1) Try to select a captain near the click.
        CaptainView hit = pickCaptain(st, e.getX(), e.getY());
        if (hit != null) {
            controller.selectCaptain(hit.id());
            repaint();
            return;
        }
        // 2) Otherwise set the selected captain's destination on the terrain.
        int sel = controller.selectedCaptainId();
        if (sel >= 0) {
            Point2D.Double tp = iso.unproject(e.getX(), e.getY());
            int tx = (int) Math.floor(tp.x + 0.5);
            int ty = (int) Math.floor(tp.y + 0.5);
            if (tx >= 0 && ty >= 0 && tx < st.mapWidth() && ty < st.mapHeight()) {
                controller.setDestination(sel, tx, ty);
                repaint();
            }
        }
    }

    private CaptainView pickCaptain(GameStateView st, int mx, int my) {
        CaptainView best = null;
        double bestD = 26 * 26; // pick radius squared
        List<CaptainView> caps = st.captains();
        for (int i = 0; i < caps.size(); i++) {
            CaptainView c = caps.get(i);
            if (!c.alive()) continue;
            Point2D.Double p = screenOf(st, c.x(), c.y());
            double dx = p.x - mx;
            double dy = (p.y - 8) - my; // aim near the banner base
            double d = dx * dx + dy * dy;
            if (d < bestD) { bestD = d; best = c; }
        }
        return best;
    }

    private Point2D.Double screenOf(GameStateView st, double tx, double ty) {
        int elev = elevationAt(st, tx, ty);
        return iso.project(tx, ty, elev);
    }

    private int elevationAt(GameStateView st, double tx, double ty) {
        int ix = clamp((int) Math.floor(tx), 0, st.mapWidth() - 1);
        int iy = clamp((int) Math.floor(ty), 0, st.mapHeight() - 1);
        TileView t = st.tile(ix, iy);
        return t == null ? 0 : t.elevation();
    }

    // ---- rendering ------------------------------------------------------

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        iso.setViewport(getWidth(), getHeight());
        GameStateView st = controller.state();

        // Terrain: painter's algorithm, back (small x+y) to front.
        int w = st.mapWidth();
        int h = st.mapHeight();
        for (int sum = 0; sum <= (w - 1) + (h - 1); sum++) {
            for (int x = 0; x < w; x++) {
                int y = sum - x;
                if (y < 0 || y >= h) continue;
                TileView t = st.tile(x, y);
                if (t == null) continue;
                if (!onScreen(st, x, y)) continue;
                iso.drawTile(g, st, t);
                if (t.hasTrees() || t.terrain() == TerrainType.FOREST) {
                    Point2D.Double p = iso.project(x, y, t.elevation());
                    SpriteFactory.tree(g, (int) p.x, (int) p.y);
                }
                if (t.hasTown()) {
                    drawTownMark(g, st, t);
                }
            }
        }

        // Townspeople.
        List<TownspersonView> people = st.townspeople();
        for (int i = 0; i < people.size(); i++) {
            TownspersonView p = people.get(i);
            Point2D.Double s = screenOf(st, p.x(), p.y());
            SpriteFactory.townsperson(g, (int) s.x, (int) s.y, p);
        }

        // Destination lines for the selected captain.
        List<CaptainView> caps = st.captains();
        for (int i = 0; i < caps.size(); i++) {
            CaptainView c = caps.get(i);
            if (c.selected() && c.hasDestination()) {
                Point2D.Double from = screenOf(st, c.x(), c.y());
                Point2D.Double to = screenOf(st, c.destX(), c.destY());
                g.setColor(new Color(240, 208, 120, 200));
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND, 1f, new float[]{6f, 6f}, 0f));
                g.drawLine((int) from.x, (int) from.y, (int) to.x, (int) to.y);
                g.setStroke(new BasicStroke(1f));
                g.fillOval((int) to.x - 4, (int) to.y - 4, 8, 8);
            }
        }

        // Captains / blocs (front to back so nearer ones overlap correctly).
        for (int i = 0; i < caps.size(); i++) {
            CaptainView c = caps.get(i);
            if (!c.alive()) continue;
            Point2D.Double s = screenOf(st, c.x(), c.y());
            SpriteFactory.captain(g, (int) s.x, (int) s.y, c);
        }

        // Pigeons in flight (command lag).
        List<PigeonView> pigeons = st.pigeons();
        for (int i = 0; i < pigeons.size(); i++) {
            PigeonView p = pigeons.get(i);
            Point2D.Double s = screenOf(st, p.x(), p.y());
            SpriteFactory.pigeon(g, (int) s.x, (int) s.y, p, animPhase);
        }

        // A wandering flock of birds for atmosphere.
        double bx = st.mapWidth() * 0.35 + 3 * Math.sin(animPhase * 0.3);
        double by = st.mapHeight() * 0.3 + 2 * Math.cos(animPhase * 0.25);
        Point2D.Double fs = screenOf(st, bx, by);
        SpriteFactory.flock(g, (int) fs.x, (int) fs.y - 40, 5, animPhase);
    }

    private void drawTownMark(Graphics2D g, GameStateView st, TileView t) {
        Point2D.Double p = iso.project(t.x(), t.y(), t.elevation());
        int cx = (int) p.x, cy = (int) p.y;
        // A little clustered keep: a few roofs.
        List<TownView> towns = st.towns();
        Color roof = UiPalette.NEUTRAL;
        for (int i = 0; i < towns.size(); i++) {
            TownView tw = towns.get(i);
            if (tw.tileX() == t.x() && tw.tileY() == t.y()) {
                roof = UiPalette.faction(tw.allegiance());
                break;
            }
        }
        g.setColor(new Color(120, 96, 70));
        g.fillRect(cx - 8, cy - 10, 16, 10);
        g.setColor(roof);
        g.fillRect(cx - 9, cy - 14, 18, 5);
        g.setColor(UiPalette.darken(roof, 0.4));
        g.drawRect(cx - 9, cy - 14, 18, 5);
    }

    private boolean onScreen(GameStateView st, int x, int y) {
        Point2D.Double p = iso.project(x, y, 0);
        int m = 80;
        return p.x > -m && p.x < getWidth() + m && p.y > -m && p.y < getHeight() + m;
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
