package com.whim.merchantprince.ui;

import com.whim.merchantprince.app.Game;
import com.whim.merchantprince.app.Screen;
import com.whim.merchantprince.engine.TravelEngine;
import com.whim.merchantprince.engine.TurnManager;
import com.whim.merchantprince.model.City;
import com.whim.merchantprince.model.Family;
import com.whim.merchantprince.model.GameState;
import com.whim.merchantprince.model.TransportUnit;
import com.whim.merchantprince.model.event.Event;
import com.whim.merchantprince.render.Palette;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * The Afro-Eurasian map (GAME_DESIGN_REFERENCE §2). Renders sea/land, every city as
 * a labelled marker (open vs closed visually distinct), and the player's fleets both
 * docked and in transit. Clicking a city focuses it and opens that city's Market.
 * Hosts the top navigation (Market/Fleet/Venice), the End Turn control, and a status
 * bar showing year, house, florins and the latest game-log line.
 */
public class MapScreen extends Screen {

    private static final int MARKER = 9;          // city marker radius
    private static final Color CLOSED = new Color(0x6B4E2E);
    private static final Color LAND_LINE = new Color(0x9C8551);
    private static final Color TRANSIT = new Color(0x3B2A66);

    private final JPanel status = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
    private final JLabel eventLine = UiKit.label("", UiKit.SMALL, Palette.CRIMSON);
    private final MapCanvas canvas = new MapCanvas();

    public MapScreen(Game game) {
        super(game);
        setLayout(new BorderLayout());

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        nav.setBackground(Palette.PARCHMENT_DK);
        nav.add(navButton("Market", Game.MARKET));
        nav.add(navButton("Fleet", Game.FLEET));
        nav.add(navButton("Venice", Game.VENICE));
        JButton end = UiKit.button("End Turn");
        end.addActionListener(e -> {
            TurnManager.endTurn(game.state, game.rng);
            if (game.state.gameOver) game.screens.show(Game.GAMEOVER);
            else { refreshStatus(); canvas.repaint(); }
        });
        nav.add(end);

        status.setBackground(Palette.PARCHMENT_DK);

        JPanel top = new JPanel(new BorderLayout());
        top.add(nav, BorderLayout.NORTH);
        top.add(status, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
    }

    private JButton navButton(String label, String card) {
        JButton b = UiKit.button(label);
        b.addActionListener(e -> game.screens.show(card));
        return b;
    }

    private void refreshStatus() {
        status.removeAll();
        GameState s = game.state;
        if (s != null) {
            Family p = s.player();
            status.add(UiKit.label("Year " + s.year + " / " + s.endYear
                    + "     House " + p.surname
                    + "     " + p.florins + " florins", UiKit.HEAD, Palette.INK));
            String latest = latestEvent(s);
            eventLine.setText(latest.isEmpty() ? "" : "⚑ " + latest);
            status.add(eventLine);
        }
        status.revalidate();
        status.repaint();
    }

    private String latestEvent(GameState s) {
        if (s.log.isEmpty()) return "";
        Event e = s.log.get(s.log.size() - 1);
        return e.message;
    }

    @Override public String name() { return Game.MAP; }

    @Override public void onShow() { refreshStatus(); canvas.repaint(); }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Palette.SEA);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    @Override public Dimension getPreferredSize() { return new Dimension(900, 680); }

    // ------------------------------------------------------------------
    // The interactive map surface.
    // ------------------------------------------------------------------
    private final class MapCanvas extends JPanel {

        MapCanvas() {
            setOpaque(true);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { onClick(e.getX(), e.getY()); }
            });
        }

        private void onClick(int mx, int my) {
            GameState s = game.state;
            if (s == null) return;
            City hit = cityAt(mx, my);
            if (hit == null) return;
            game.focusCityId = hit.id;
            game.returnTo = Game.MAP;
            game.screens.show(Game.MARKET);
        }

        private City cityAt(int mx, int my) {
            GameState s = game.state;
            City best = null;
            double bestD = (MARKER + 6) * (MARKER + 6);
            for (City c : s.cities) {
                double dx = mx - c.x, dy = my - c.y;
                double d = dx * dx + dy * dy;
                if (d <= bestD) { bestD = d; best = c; }
            }
            return best;
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(Palette.SEA);
            g2.fillRect(0, 0, getWidth(), getHeight());

            GameState s = game.state;
            if (s == null) return;

            drawLandmasses(g2);
            drawFleets(g2, s);
            drawCities(g2, s);
            drawLegend(g2);
        }

        /** Programmatic "continent" blobs — placeholder art, no external images. */
        private void drawLandmasses(Graphics2D g2) {
            g2.setColor(Palette.LAND);
            // Europe / North Africa mass (west/centre)
            g2.fillRoundRect(40, 60, 420, 220, 90, 90);
            // Sub-Saharan / East Africa
            g2.fillRoundRect(70, 250, 260, 220, 80, 80);
            // Asia mass (east)
            g2.fillRoundRect(470, 40, 400, 300, 100, 100);
            // India / Indian Ocean shore
            g2.fillRoundRect(560, 280, 180, 150, 70, 70);
            g2.setColor(LAND_LINE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(40, 60, 420, 220, 90, 90);
            g2.drawRoundRect(70, 250, 260, 220, 80, 80);
            g2.drawRoundRect(470, 40, 400, 300, 100, 100);
            g2.drawRoundRect(560, 280, 180, 150, 70, 70);
        }

        private void drawCities(Graphics2D g2, GameState s) {
            g2.setFont(UiKit.SMALL);
            for (City c : s.cities) {
                int r = MARKER;
                if (c.open) {
                    g2.setColor(Palette.GOLD);
                    g2.fillOval(c.x - r, c.y - r, r * 2, r * 2);
                    g2.setColor(Palette.INK);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawOval(c.x - r, c.y - r, r * 2, r * 2);
                } else {
                    // closed city: hollow, dark, dashed ring
                    g2.setColor(Palette.PARCHMENT);
                    g2.fillOval(c.x - r, c.y - r, r * 2, r * 2);
                    g2.setColor(CLOSED);
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                            10f, new float[] {4f, 3f}, 0f));
                    g2.drawOval(c.x - r, c.y - r, r * 2, r * 2);
                    // small lock tick
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawLine(c.x - 3, c.y, c.x + 3, c.y);
                }
                g2.setColor(Palette.INK);
                g2.setFont(UiKit.SMALL);
                g2.drawString(c.name, c.x + r + 4, c.y + 4);
                if (!c.open) {
                    g2.setColor(CLOSED);
                    g2.setFont(new Font("Serif", Font.ITALIC, 11));
                    g2.drawString("(closed)", c.x + r + 4, c.y + 17);
                }
            }
        }

        private void drawFleets(Graphics2D g2, GameState s) {
            Family p = s.player();
            if (p == null) return;
            for (TransportUnit u : s.unitsOf(p.id)) {
                if (u.inTransit()) drawInTransit(g2, s, u);
                else drawDocked(g2, s, u);
            }
        }

        private void drawDocked(Graphics2D g2, GameState s, TransportUnit u) {
            City at = s.city(u.locationCityId);
            if (at == null) return;
            int fx = at.x - MARKER - 6;
            int fy = at.y - MARKER - 6;
            drawShipGlyph(g2, fx, fy, Palette.CRIMSON);
        }

        private void drawInTransit(Graphics2D g2, GameState s, TransportUnit u) {
            City from = s.city(u.locationCityId);
            City to = s.city(u.destinationCityId);
            if (from == null || to == null) return;

            int total = Math.max(1, TravelEngine.turnsBetween(from, to, u.type));
            double progress = 1.0 - Math.min(1.0, Math.max(0.0, (double) u.turnsRemaining / total));

            Stroke old = g2.getStroke();
            g2.setColor(TRANSIT);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    10f, new float[] {6f, 5f}, 0f));
            g2.drawLine(from.x, from.y, to.x, to.y);
            g2.setStroke(old);

            int gx = (int) Math.round(from.x + (to.x - from.x) * progress);
            int gy = (int) Math.round(from.y + (to.y - from.y) * progress);
            drawShipGlyph(g2, gx - 6, gy - 6, TRANSIT);

            g2.setColor(TRANSIT);
            g2.setFont(new Font("Serif", Font.BOLD, 11));
            g2.drawString(u.turnsRemaining + "t", gx + 8, gy - 6);
        }

        /** A tiny sail glyph for a fleet marker. */
        private void drawShipGlyph(Graphics2D g2, int x, int y, Color color) {
            g2.setColor(color);
            g2.fillRect(x, y + 8, 12, 3);            // hull
            g2.drawLine(x + 6, y, x + 6, y + 8);      // mast
            g2.fillPolygon(new int[] {x + 6, x + 12, x + 6},
                    new int[] {y, y + 4, y + 8}, 3);   // sail
        }

        private void drawLegend(Graphics2D g2) {
            int lx = 16, ly = getHeight() - 78;
            g2.setColor(Palette.PANEL);
            g2.fillRoundRect(lx - 8, ly - 18, 190, 74, 12, 12);
            g2.setColor(Palette.PANEL_LINE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(lx - 8, ly - 18, 190, 74, 12, 12);

            g2.setFont(UiKit.SMALL);
            g2.setColor(Palette.GOLD);
            g2.fillOval(lx, ly - 8, 12, 12);
            g2.setColor(Palette.INK);
            g2.drawString("Open city", lx + 20, ly + 2);

            g2.setColor(Palette.PARCHMENT);
            g2.fillOval(lx, ly + 10, 12, 12);
            g2.setColor(CLOSED);
            g2.drawOval(lx, ly + 10, 12, 12);
            g2.setColor(Palette.INK);
            g2.drawString("Closed city", lx + 20, ly + 20);

            drawShipGlyph(g2, lx, ly + 26, Palette.CRIMSON);
            g2.setColor(Palette.INK);
            g2.drawString("Your fleet", lx + 20, ly + 38);
        }
    }
}
