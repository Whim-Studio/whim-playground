package com.whim.ttr.ui;

import com.whim.ttr.api.CardColor;
import com.whim.ttr.api.RouteKind;
import com.whim.ttr.domain.Board;
import com.whim.ttr.domain.City;
import com.whim.ttr.domain.GameState;
import com.whim.ttr.domain.Player;
import com.whim.ttr.domain.Route;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Procedurally draws the Ticket to Ride: Europe map with {@link Graphics2D}.
 *
 * <p>Cities are labeled circular nodes positioned at {@code City.x()/y()} in a
 * 0..1000 virtual space, uniformly scaled (letter-boxed) into the panel. Routes
 * are drawn as a run of segmented colored boxes along the line joining their two
 * cities; the two edges of a double route are drawn with a perpendicular offset
 * so they never overlap. A claimed route is recolored to its owner's token.</p>
 *
 * <p>The panel is purely a view + hit-tester: a click resolves to the nearest
 * route (attempt claim) or the enclosing city node (build station) and is
 * forwarded to a {@link ClickListener}. All engine work happens elsewhere.</p>
 */
public class BoardPanel extends JPanel {

    /** Notified when the user clicks a route or a city node. */
    public interface ClickListener {
        void onRouteClicked(String routeId);
        void onCityClicked(String cityId);
    }

    private static final double VIRTUAL = 1000.0;
    private static final int NODE_R = 11;          // city node radius (virtual px)
    private static final double ROUTE_CLICK_TOL = 12.0;

    private final GameState state;
    private ClickListener listener;

    // Last-painted geometry, in device pixels, rebuilt every paint for hit-testing.
    private final Map<String, Point2D> cityPix = new HashMap<String, Point2D>();
    private final List<RouteHit> routeHits = new ArrayList<RouteHit>();

    private double scale = 1.0;
    private double offX = 0.0;
    private double offY = 0.0;

    public BoardPanel(GameState state) {
        this.state = state;
        setBackground(new Color(196, 220, 236));
        setPreferredSize(new Dimension(1000, 700));
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { handleClick(e.getX(), e.getY()); }
        });
    }

    public void setClickListener(ClickListener l) {
        this.listener = l;
    }

    // ---- painting -----------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        computeTransform();
        cityPix.clear();
        routeHits.clear();

        Board board = state.board();
        if (board == null) {
            g2.dispose();
            return;
        }

        drawRoutes(g2, board);
        drawCities(g2, board);
        drawTitle(g2);

        g2.dispose();
    }

    private void computeTransform() {
        int w = getWidth();
        int h = getHeight();
        double s = Math.min(w / VIRTUAL, h / VIRTUAL);
        this.scale = s;
        this.offX = (w - VIRTUAL * s) / 2.0;
        this.offY = (h - VIRTUAL * s) / 2.0;
    }

    private double px(double vx) { return offX + vx * scale; }
    private double py(double vy) { return offY + vy * scale; }

    private void drawRoutes(Graphics2D g2, Board board) {
        Collection<Route> routes = board.routes();
        // Group parallel edges (same unordered city pair) to offset double routes.
        Map<String, List<Route>> groups = new HashMap<String, List<Route>>();
        for (Route r : routes) {
            String key = pairKey(r.cityA(), r.cityB());
            List<Route> list = groups.get(key);
            if (list == null) {
                list = new ArrayList<Route>();
                groups.put(key, list);
            }
            list.add(r);
        }

        for (List<Route> group : groups.values()) {
            int n = group.size();
            for (int i = 0; i < n; i++) {
                Route r = group.get(i);
                double offset = 0.0;
                if (n > 1) {
                    // spread edges symmetrically around the center line
                    offset = (i - (n - 1) / 2.0) * (NODE_R * 1.15);
                }
                drawRoute(g2, board, r, offset);
            }
        }
    }

    private void drawRoute(Graphics2D g2, Board board, Route r, double perpOffsetVirtual) {
        City a = board.city(r.cityA());
        City b = board.city(r.cityB());
        if (a == null || b == null) {
            return;
        }
        double ax = px(a.x()), ay = py(a.y());
        double bx = px(b.x()), by = py(b.y());
        double dx = bx - ax, dy = by - ay;
        double len = Math.hypot(dx, dy);
        if (len < 1e-6) {
            return;
        }
        double ux = dx / len, uy = dy / len;      // unit along
        double nx = -uy, ny = ux;                  // unit perpendicular
        double off = perpOffsetVirtual * scale;
        ax += nx * off; ay += ny * off;
        bx += nx * off; by += ny * off;

        // Leave room at each end for the city nodes.
        double pad = NODE_R * scale + 3;
        double sx = ax + ux * pad, sy = ay + uy * pad;
        double ex = bx - ux * pad, ey = by - uy * pad;

        int segs = Math.max(1, r.length());
        Integer owner = r.ownerId();
        Color fill = (owner != null) ? ownerColor(owner) : UiColors.of(r.color());

        double segLen = Math.hypot(ex - sx, ey - sy) / segs;
        double boxW = Math.max(3.0, segLen * 0.72);
        double boxH = Math.max(6.0, NODE_R * scale * 0.9);

        Line2D fullLine = new Line2D.Double(sx, sy, ex, ey);
        routeHits.add(new RouteHit(r.id(), fullLine));

        double angle = Math.atan2(ey - sy, ex - sx);
        for (int s = 0; s < segs; s++) {
            double t = (s + 0.5) / segs;
            double cx = sx + (ex - sx) * t;
            double cy = sy + (ey - sy) * t;
            drawSegmentBox(g2, cx, cy, angle, boxW, boxH, fill, r);
        }
    }

    private void drawSegmentBox(Graphics2D g2, double cx, double cy, double angle,
                                double w, double h, Color fill, Route r) {
        Graphics2D gg = (Graphics2D) g2.create();
        gg.translate(cx, cy);
        gg.rotate(angle);
        java.awt.geom.RoundRectangle2D box =
                new java.awt.geom.RoundRectangle2D.Double(-w / 2, -h / 2, w, h, 4, 4);
        gg.setColor(fill);
        gg.fill(box);
        // Tunnels get a dashed border, ferries a heavier one, plus a loco pip.
        if (r.kind() == RouteKind.TUNNEL) {
            gg.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    1f, new float[] { 3f, 2f }, 0f));
            gg.setColor(new Color(30, 30, 30));
        } else if (r.kind() == RouteKind.FERRY) {
            gg.setStroke(new BasicStroke(1.8f));
            gg.setColor(new Color(20, 60, 120));
        } else {
            gg.setStroke(new BasicStroke(1f));
            gg.setColor(new Color(40, 40, 40, 160));
        }
        gg.draw(box);
        gg.dispose();
    }

    private void drawCities(Graphics2D g2, Board board) {
        Font labelFont = getFont().deriveFont(Font.BOLD, (float) Math.max(9.0, 12.0 * scale));
        g2.setFont(labelFont);
        int r = (int) Math.round(NODE_R * scale);
        for (City c : board.cities()) {
            double cx = px(c.x());
            double cy = py(c.y());
            cityPix.put(c.id(), new Point2D.Double(cx, cy));

            Color ring = stationRing(c.id());
            g2.setColor(new Color(250, 250, 250));
            g2.fillOval((int) (cx - r), (int) (cy - r), 2 * r, 2 * r);
            g2.setStroke(new BasicStroke(ring != null ? 3.2f : 1.8f));
            g2.setColor(ring != null ? ring : new Color(60, 60, 60));
            g2.drawOval((int) (cx - r), (int) (cy - r), 2 * r, 2 * r);

            g2.setColor(new Color(20, 20, 20));
            String name = c.name();
            int tw = g2.getFontMetrics().stringWidth(name);
            g2.setColor(new Color(255, 255, 255, 200));
            g2.fillRect((int) (cx - tw / 2 - 2), (int) (cy - r - 15), tw + 4, 13);
            g2.setColor(new Color(20, 20, 20));
            g2.drawString(name, (int) (cx - tw / 2), (int) (cy - r - 4));
        }
    }

    private void drawTitle(Graphics2D g2) {
        g2.setColor(new Color(30, 50, 80));
        g2.setFont(getFont().deriveFont(Font.BOLD, 16f));
        g2.drawString("Ticket to Ride — Europe", 12, 22);
    }

    /** Owner token color, or a station ring for cities with a station. */
    private Color ownerColor(int ownerId) {
        Player p = state.player(ownerId);
        if (p != null && p.token() != null) {
            return p.token();
        }
        return new Color(90, 90, 90);
    }

    private Color stationRing(String cityId) {
        for (Player p : state.players()) {
            List<String> st = p.stationCities();
            if (st != null && st.contains(cityId)) {
                return p.token();
            }
        }
        return null;
    }

    // ---- hit testing --------------------------------------------------------

    private void handleClick(int mx, int my) {
        if (listener == null) {
            return;
        }
        // City nodes take priority (they sit on top of route ends).
        double rNode = NODE_R * scale + 2;
        for (Map.Entry<String, Point2D> e : cityPix.entrySet()) {
            if (e.getValue().distance(mx, my) <= rNode) {
                listener.onCityClicked(e.getKey());
                return;
            }
        }
        String best = null;
        double bestDist = ROUTE_CLICK_TOL;
        for (RouteHit rh : routeHits) {
            double d = rh.line.ptSegDist(mx, my);
            if (d < bestDist) {
                bestDist = d;
                best = rh.routeId;
            }
        }
        if (best != null) {
            listener.onRouteClicked(best);
        }
    }

    private static String pairKey(String a, String b) {
        return (a.compareTo(b) <= 0) ? a + " " + b : b + " " + a;
    }

    private static final class RouteHit {
        final String routeId;
        final Line2D line;
        RouteHit(String routeId, Line2D line) {
            this.routeId = routeId;
            this.line = line;
        }
    }
}
