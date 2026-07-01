package com.whim.warroom.ui;

import com.whim.warroom.domain.Biome;
import com.whim.warroom.domain.Faction;
import com.whim.warroom.domain.MapMarker;
import com.whim.warroom.domain.MapState;
import com.whim.warroom.domain.Route;
import com.whim.warroom.domain.SandboxState;
import com.whim.warroom.domain.SimSnapshot;
import com.whim.warroom.domain.Stance;
import com.whim.warroom.domain.TerrainTile;
import com.whim.warroom.domain.Unit;
import com.whim.warroom.domain.UnitType;
import com.whim.warroom.domain.Vec2;
import com.whim.warroom.domain.Waypoint;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/**
 * CENTER battlefield view. Renders the top-down 2D map with {@link Graphics2D}
 * (terrain, units, routes, markers, blasts) and owns ALL mouse input. In Battle
 * Mode it draws from the latest {@link SimSnapshot}; in Editor Mode from the live
 * {@link SandboxState}. Applies a pan/zoom world→screen transform.
 */
public final class BattlefieldPanel extends JPanel
        implements MouseListener, MouseMotionListener, MouseWheelListener {

    private final SandboxController ctl;

    // world→screen: screen = world*zoom + offset
    private double zoom = 1.0;
    private double offX = 40, offY = 40;

    // panning (middle button drag)
    private boolean panning;
    private int panStartX, panStartY;
    private double panStartOffX, panStartOffY;

    // hover / interaction state
    private int hoverWorldValid = 0;
    private double hoverWX, hoverWY;

    // drag-box selection (editor SELECT tool)
    private boolean boxing;
    private double boxX0, boxY0, boxX1, boxY1;

    // route drawing (editor ROUTE tool)
    private int routeUnitId = -1;
    private final List<Vec2> routePts = new ArrayList<Vec2>();

    public BattlefieldPanel(SandboxController ctl) {
        this.ctl = ctl;
        setBackground(ThemeUI.BG_DEEP);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }

    // ---------- camera helpers ----------
    private double sx(double wx) { return wx * zoom + offX; }
    private double sy(double wy) { return wy * zoom + offY; }
    private double wx(double screenX) { return (screenX - offX) / zoom; }
    private double wy(double screenY) { return (screenY - offY) / zoom; }

    /** Fit the whole map into the viewport with a small margin. */
    public void fitToMap() {
        MapState map = ctl.getState().getMap();
        int w = getWidth() <= 0 ? 800 : getWidth();
        int h = getHeight() <= 0 ? 600 : getHeight();
        double zx = (w - 40) / Math.max(1.0, map.worldWidth());
        double zy = (h - 40) / Math.max(1.0, map.worldHeight());
        zoom = Math.max(0.15, Math.min(zx, zy));
        offX = (w - map.worldWidth() * zoom) / 2;
        offY = (h - map.worldHeight() * zoom) / 2;
        repaint();
    }

    // ================= rendering =================
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        drawTerrain(g2);
        drawMarkers(g2);

        if (ctl.getMode() == SandboxController.Mode.BATTLE) {
            drawRouteGhosts(g2);
            drawBattle(g2);
        } else {
            drawEditorUnitsAndRoutes(g2);
            drawRouteInProgress(g2);
            drawSelectionBox(g2);
        }
        drawHud(g2);
    }

    private void drawTerrain(Graphics2D g2) {
        MapState map = ctl.getState().getMap();
        double ts = MapState.TILE_SIZE * zoom;
        for (int c = 0; c < map.getCols(); c++) {
            for (int r = 0; r < map.getRows(); r++) {
                TerrainTile t = map.tile(c, r);
                Biome b = t.getBiome();
                double elev = t.getElevation();
                // elevation shading: darken lows, brighten highs around 0.5 midpoint
                Color base = b.color();
                Color shade = elev >= 0.5
                        ? ThemeUI.mix(base, Color.WHITE, (elev - 0.5) * 0.5)
                        : ThemeUI.mix(base, Color.BLACK, (0.5 - elev) * 0.7);
                g2.setColor(shade);
                double x = sx(c * MapState.TILE_SIZE), y = sy(r * MapState.TILE_SIZE);
                g2.fillRect((int) Math.floor(x), (int) Math.floor(y),
                        (int) Math.ceil(ts) + 1, (int) Math.ceil(ts) + 1);
            }
        }
        // grid lines (only when zoomed in enough to be legible)
        if (ts >= 10) {
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(ThemeUI.GRID_LINE);
            for (int c = 0; c <= map.getCols(); c++) {
                int x = (int) sx(c * MapState.TILE_SIZE);
                g2.drawLine(x, (int) sy(0), x, (int) sy(map.worldHeight()));
            }
            for (int r = 0; r <= map.getRows(); r++) {
                int y = (int) sy(r * MapState.TILE_SIZE);
                g2.drawLine((int) sx(0), y, (int) sx(map.worldWidth()), y);
            }
        }
        // world border
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(ThemeUI.BORDER);
        g2.drawRect((int) sx(0), (int) sy(0),
                (int) (map.worldWidth() * zoom), (int) (map.worldHeight() * zoom));
    }

    private void drawMarkers(Graphics2D g2) {
        for (MapMarker m : ctl.getState().getMarkers()) {
            double x = sx(m.getPos().x), y = sy(m.getPos().y);
            g2.setColor(ThemeUI.alpha(m.getColor(), 210));
            int[] px = {(int) x, (int) (x - 7), (int) (x + 7)};
            int[] py = {(int) y, (int) (y - 16), (int) (y - 16)};
            g2.fillPolygon(px, py, 3);
            g2.setColor(Color.WHITE);
            g2.fillOval((int) x - 3, (int) y - 19, 6, 6);
            g2.setFont(ThemeUI.UI_SMALL);
            g2.setColor(ThemeUI.TEXT);
            g2.drawString(m.getLabel(), (int) x + 9, (int) y - 8);
        }
    }

    private void drawEditorUnitsAndRoutes(Graphics2D g2) {
        // routes first (under units)
        for (Unit u : ctl.getState().getUnits()) {
            Route rt = u.getRoute();
            if (rt == null || rt.isEmpty()) continue;
            drawRoute(g2, u.getPos(), rt, u.getFaction());
        }
        for (Unit u : ctl.getState().getUnits()) {
            drawUnitGlyph(g2, u.getPos().x, u.getPos().y, u.getHeading(),
                    u.getFaction(), u.getType(),
                    u.getHealth() / Math.max(1, u.getType().getMaxHealth()),
                    u.getMorale() / Math.max(1, u.getType().getMaxMorale()),
                    u.isRouted(), u.getStance(), ctl.isSelected(u.getId()), 0);
        }
    }

    private void drawRoute(Graphics2D g2, Vec2 start, Route rt, Faction fac) {
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(ThemeUI.alpha(fac.color(), 150));
        Vec2 prev = start;
        for (int i = 0; i < rt.getWaypoints().size(); i++) {
            Waypoint w = rt.getWaypoints().get(i);
            g2.draw(new Line2D.Double(sx(prev.x), sy(prev.y), sx(w.getPos().x), sy(w.getPos().y)));
            prev = w.getPos();
        }
        for (int i = 0; i < rt.getWaypoints().size(); i++) {
            Waypoint w = rt.getWaypoints().get(i);
            double x = sx(w.getPos().x), y = sy(w.getPos().y);
            if (w.isDetonation()) {
                g2.setColor(ThemeUI.DANGER);
                g2.fillOval((int) x - 5, (int) y - 5, 10, 10);
                g2.setColor(ThemeUI.BLAST_CORE);
                g2.drawOval((int) (x - w.getBlastRadius() * zoom), (int) (y - w.getBlastRadius() * zoom),
                        (int) (w.getBlastRadius() * 2 * zoom), (int) (w.getBlastRadius() * 2 * zoom));
            } else {
                g2.setColor(ThemeUI.ROUTE_LINE);
                g2.fillOval((int) x - 3, (int) y - 3, 6, 6);
            }
            g2.setColor(ThemeUI.TEXT_DIM);
            g2.setFont(ThemeUI.UI_SMALL);
            g2.drawString("t" + w.getArrivalTick(), (int) x + 6, (int) y - 6);
        }
    }

    /** Faint route ghosts drawn in Battle Mode from the editor scenario. */
    private void drawRouteGhosts(Graphics2D g2) {
        for (Unit u : ctl.getState().getUnits()) {
            Route rt = u.getRoute();
            if (rt == null || rt.isEmpty()) continue;
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[]{5f, 5f}, 0f));
            g2.setColor(ThemeUI.alpha(u.getFaction().color(), 70));
            Vec2 prev = u.getPos();
            for (int i = 0; i < rt.getWaypoints().size(); i++) {
                Vec2 p = rt.getWaypoints().get(i).getPos();
                g2.draw(new Line2D.Double(sx(prev.x), sy(prev.y), sx(p.x), sy(p.y)));
                prev = p;
            }
        }
    }

    private void drawBattle(Graphics2D g2) {
        SimSnapshot snap = ctl.getLatestSnapshot();
        if (snap == null) return;
        // blasts under units
        for (int i = 0; i < snap.getBlasts().size(); i++) {
            SimSnapshot.BlastView b = snap.getBlasts().get(i);
            double x = sx(b.x), y = sy(b.y), rad = b.radius * zoom;
            float a = (float) Math.max(0, 1 - b.age);
            g2.setColor(ThemeUI.alpha(ThemeUI.BLAST_EDGE, (int) (140 * a)));
            g2.fill(new Ellipse2D.Double(x - rad, y - rad, rad * 2, rad * 2));
            double core = rad * (0.4 + 0.6 * b.age);
            g2.setColor(ThemeUI.alpha(ThemeUI.BLAST_CORE, (int) (200 * a)));
            g2.fill(new Ellipse2D.Double(x - core, y - core, core * 2, core * 2));
        }
        for (int i = 0; i < snap.getUnits().size(); i++) {
            SimSnapshot.UnitView v = snap.getUnits().get(i);
            if (!v.alive) continue;
            UnitType type = com.whim.warroom.domain.UnitCatalog.byId(v.typeId);
            drawUnitGlyph(g2, v.x, v.y, v.heading, v.faction, type,
                    v.health / Math.max(1, v.maxHealth),
                    v.morale / Math.max(1, v.maxMorale),
                    v.routed, v.stance, false, snap.getTick());
        }
    }

    /**
     * One unit glyph: body disk (faction color, morale-tinted), heading tick,
     * health ring, stance pip, selection halo, routed flash.
     */
    private void drawUnitGlyph(Graphics2D g2, double wxp, double wyp, double heading,
                               Faction fac, UnitType type, double healthFrac, double moraleFrac,
                               boolean routed, Stance stance, boolean selected, int tick) {
        double x = sx(wxp), y = sy(wyp);
        double rad = Math.max(6, 9 * zoom);
        healthFrac = clamp01(healthFrac);
        moraleFrac = clamp01(moraleFrac);

        if (selected) {
            g2.setColor(ThemeUI.alpha(ThemeUI.SELECT, 200));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new Ellipse2D.Double(x - rad - 4, y - rad - 4, (rad + 4) * 2, (rad + 4) * 2));
        }

        // body: faction color desaturated toward gray as morale drops
        Color body = ThemeUI.mix(new Color(90, 96, 104), fac == null ? Color.GRAY : fac.color(), 0.35 + 0.65 * moraleFrac);
        if (routed && (tick / 6) % 2 == 0) body = ThemeUI.mix(body, Color.WHITE, 0.6); // panic flash
        g2.setColor(body);
        g2.fill(new Ellipse2D.Double(x - rad, y - rad, rad * 2, rad * 2));

        // heading tick
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(Color.WHITE);
        g2.draw(new Line2D.Double(x, y, x + Math.cos(heading) * rad, y + Math.sin(heading) * rad));

        // health ring
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(ThemeUI.alpha(Color.BLACK, 120));
        g2.draw(new Ellipse2D.Double(x - rad - 2, y - rad - 2, (rad + 2) * 2, (rad + 2) * 2));
        Color ring = healthFrac > 0.5 ? ThemeUI.mix(ThemeUI.WARN, new Color(90, 210, 110), (healthFrac - 0.5) * 2)
                                      : ThemeUI.mix(ThemeUI.DANGER, ThemeUI.WARN, healthFrac * 2);
        g2.setColor(ring);
        int deg = (int) Math.round(360 * healthFrac);
        g2.drawArc((int) (x - rad - 2), (int) (y - rad - 2), (int) ((rad + 2) * 2), (int) ((rad + 2) * 2), 90, -deg);

        // stance pip
        if (stance != null) {
            Color pip = stance == Stance.OFFENSIVE ? ThemeUI.DANGER
                      : stance == Stance.RETREAT ? ThemeUI.ACCENT : ThemeUI.WARN;
            g2.setColor(pip);
            g2.fill(new Ellipse2D.Double(x + rad - 3, y - rad, 6, 6));
        }

        if (routed) {
            g2.setColor(ThemeUI.DANGER);
            g2.setFont(ThemeUI.UI_BOLD);
            g2.drawString("!", (int) (x - 2), (int) (y - rad - 6));
        }
    }

    private void drawRouteInProgress(Graphics2D g2) {
        if (routeUnitId < 0 || routePts.isEmpty()) return;
        Unit u = ctl.getState().unit(routeUnitId);
        if (u == null) return;
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1f, new float[]{6f, 4f}, 0f));
        g2.setColor(ThemeUI.SELECT);
        Vec2 prev = u.getPos();
        for (int i = 0; i < routePts.size(); i++) {
            Vec2 p = routePts.get(i);
            g2.draw(new Line2D.Double(sx(prev.x), sy(prev.y), sx(p.x), sy(p.y)));
            g2.fillOval((int) sx(p.x) - 3, (int) sy(p.y) - 3, 6, 6);
            prev = p;
        }
        // rubber-band to cursor
        if (hoverWorldValid == 1) {
            g2.setColor(ThemeUI.alpha(ThemeUI.SELECT, 120));
            g2.draw(new Line2D.Double(sx(prev.x), sy(prev.y), sx(hoverWX), sy(hoverWY)));
        }
    }

    private void drawSelectionBox(Graphics2D g2) {
        if (!boxing) return;
        double x = Math.min(sx(boxX0), sx(boxX1)), y = Math.min(sy(boxY0), sy(boxY1));
        double w = Math.abs(sx(boxX1) - sx(boxX0)), h = Math.abs(sy(boxY1) - sy(boxY0));
        g2.setColor(ThemeUI.alpha(ThemeUI.SELECT, 40));
        g2.fillRect((int) x, (int) y, (int) w, (int) h);
        g2.setColor(ThemeUI.SELECT);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect((int) x, (int) y, (int) w, (int) h);
    }

    private void drawHud(Graphics2D g2) {
        g2.setFont(ThemeUI.HUD);
        String line;
        if (ctl.getMode() == SandboxController.Mode.BATTLE) {
            SimSnapshot s = ctl.getLatestSnapshot();
            int tick = s == null ? 0 : s.getTick();
            line = "BATTLE  tick " + tick + "  x" + trim(ctl.getEngine().getSpeed())
                    + (ctl.getEngine().isPlaying() ? "  ▶" : "  ❙❙")
                    + (s != null && s.isFinished() ? "  — ENGAGEMENT RESOLVED" : "");
        } else {
            line = "EDITOR  tool: " + ctl.getTool()
                    + (ctl.getBrushType() != null ? "  unit: " + ctl.getBrushType().getName() : "")
                    + "  " + ctl.getBrushFaction() + "/" + ctl.getBrushStance();
        }
        g2.setColor(ThemeUI.alpha(Color.BLACK, 150));
        g2.fillRect(0, 0, getWidth(), 22);
        g2.setColor(ThemeUI.ACCENT);
        g2.drawString(line, 10, 16);
    }

    // ================= mouse input =================
    public void mousePressed(MouseEvent e) {
        requestFocusInWindow();
        double wxp = wx(e.getX()), wyp = wy(e.getY());
        if (e.getButton() == MouseEvent.BUTTON2 || e.isAltDown()) {
            panning = true;
            panStartX = e.getX(); panStartY = e.getY();
            panStartOffX = offX; panStartOffY = offY;
            return;
        }
        if (ctl.getMode() == SandboxController.Mode.BATTLE) return; // playback: no editing

        SandboxController.Tool tool = ctl.getTool();
        if (e.getButton() == MouseEvent.BUTTON3) {
            // right-click finalizes a route
            finalizeRoute();
            return;
        }
        switch (tool) {
            case PLACE_UNIT:
                int id = ctl.placeUnit(wxp, wyp);
                ctl.selectOnly(id);
                break;
            case MARKER:
                ctl.dropMarker(wxp, wyp);
                break;
            case PAINT_TERRAIN:
                ctl.paintTerrain(wxp, wyp);
                break;
            case SELECT:
                boxing = true; boxX0 = boxX1 = wxp; boxY0 = boxY1 = wyp;
                break;
            case ROUTE:
                if (routeUnitId < 0) {
                    int picked = ctl.pickUnit(wxp, wyp, 14 / zoom);
                    if (picked >= 0) { routeUnitId = picked; routePts.clear(); ctl.selectOnly(picked); }
                } else {
                    routePts.add(new Vec2(wxp, wyp));
                }
                break;
        }
        repaint();
    }

    public void mouseDragged(MouseEvent e) {
        hoverWorldValid = 1; hoverWX = wx(e.getX()); hoverWY = wy(e.getY());
        if (panning) {
            offX = panStartOffX + (e.getX() - panStartX);
            offY = panStartOffY + (e.getY() - panStartY);
            repaint();
            return;
        }
        if (ctl.getMode() == SandboxController.Mode.BATTLE) return;
        if (ctl.getTool() == SandboxController.Tool.SELECT && boxing) {
            boxX1 = wx(e.getX()); boxY1 = wy(e.getY());
        } else if (ctl.getTool() == SandboxController.Tool.PAINT_TERRAIN) {
            ctl.paintTerrain(wx(e.getX()), wy(e.getY()));
        }
        repaint();
    }

    public void mouseReleased(MouseEvent e) {
        if (panning) { panning = false; return; }
        if (ctl.getMode() == SandboxController.Mode.BATTLE) return;
        if (ctl.getTool() == SandboxController.Tool.SELECT && boxing) {
            boxing = false;
            ctl.selectInBox(boxX0, boxY0, boxX1, boxY1);
            repaint();
        }
    }

    private void finalizeRoute() {
        if (routeUnitId >= 0 && !routePts.isEmpty()) {
            ctl.assignRoute(routeUnitId, new ArrayList<Vec2>(routePts));
        }
        routeUnitId = -1;
        routePts.clear();
        repaint();
    }

    public void mouseMoved(MouseEvent e) {
        hoverWorldValid = 1; hoverWX = wx(e.getX()); hoverWY = wy(e.getY());
        if (routeUnitId >= 0) repaint();
        // top-edge hover restores chrome when in cinema mode
        if (e.getY() <= 3) ctl.getField().requestChromeRestore();
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        double factor = e.getWheelRotation() < 0 ? 1.1 : 1 / 1.1;
        double newZoom = Math.max(0.15, Math.min(6.0, zoom * factor));
        // zoom around cursor
        double cwx = wx(e.getX()), cwy = wy(e.getY());
        zoom = newZoom;
        offX = e.getX() - cwx * zoom;
        offY = e.getY() - cwy * zoom;
        repaint();
    }

    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) { hoverWorldValid = 0; }

    // cinema-mode callback hook (set by WarRoomFrame)
    private Runnable chromeRestore;
    public void setChromeRestore(Runnable r) { chromeRestore = r; }
    public void requestChromeRestore() { if (chromeRestore != null) chromeRestore.run(); }

    private static double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
    private static String trim(double d) {
        if (d == Math.floor(d)) return String.valueOf((int) d);
        return String.valueOf(d);
    }
}
