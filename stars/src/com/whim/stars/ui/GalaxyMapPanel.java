package com.whim.stars.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.whim.stars.model.Fleet;
import com.whim.stars.model.Galaxy;
import com.whim.stars.model.Planet;
import com.whim.stars.model.Player;

/**
 * The Graphics2D-rendered star map — the central view of the game. It draws
 * every planet (coloured by owner) and fleet, supports panning (drag) and
 * zooming (mouse wheel), and reports planet selection to a listener so the
 * {@link CommandPanel} can show a report.
 *
 * <p>Pure View: it reads the {@link Galaxy} model but never mutates it.
 */
public final class GalaxyMapPanel extends JPanel {

    /** Notified when the user clicks a planet (or empty space -> null). */
    public interface SelectionListener {
        void onPlanetSelected(Planet planet);
    }

    private static final Color SPACE = new Color(8, 10, 24);
    private static final Color GRID = new Color(30, 34, 60);
    private static final Color NEUTRAL = new Color(150, 150, 160);
    private static final Color LABEL = new Color(200, 205, 225);

    private Galaxy galaxy;
    private Planet selected;
    private SelectionListener selectionListener;

    private double scale = 0.8;
    private double offsetX = 40;
    private double offsetY = 40;

    private int lastDragX;
    private int lastDragY;
    private boolean dragging;

    public GalaxyMapPanel() {
        setBackground(SPACE);
        setPreferredSize(new Dimension(720, 720));
        MouseHandler h = new MouseHandler();
        addMouseListener(h);
        addMouseMotionListener(h);
        addMouseWheelListener(h);
    }

    public void setGalaxy(Galaxy galaxy) {
        this.galaxy = galaxy;
        this.selected = null;
        repaint();
    }

    public void setSelectionListener(SelectionListener listener) {
        this.selectionListener = listener;
    }

    public Planet selected() {
        return selected;
    }

    public void setSelected(Planet planet) {
        this.selected = planet;
        repaint();
    }

    private double toScreenX(double worldX) {
        return worldX * scale + offsetX;
    }

    private double toScreenY(double worldY) {
        return worldY * scale + offsetY;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (galaxy == null) {
            return;
        }
        drawGrid(g2);
        drawFleets(g2);
        drawPlanets(g2);
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(GRID);
        g2.setStroke(new BasicStroke(1f));
        int step = 100;
        for (int w = 0; w <= galaxy.width(); w += step) {
            int sx = (int) toScreenX(w);
            int sy = (int) toScreenY(w);
            g2.drawLine(sx, (int) toScreenY(0), sx, (int) toScreenY(galaxy.width()));
            g2.drawLine((int) toScreenX(0), sy, (int) toScreenX(galaxy.width()), sy);
        }
    }

    private void drawPlanets(Graphics2D g2) {
        for (Planet p : galaxy.planets()) {
            int sx = (int) toScreenX(p.x());
            int sy = (int) toScreenY(p.y());
            int r = planetRadius(p);

            Color color = NEUTRAL;
            if (p.isColonized()) {
                Player owner = galaxy.player(p.ownerId());
                color = owner != null ? new Color(owner.colorRgb()) : NEUTRAL;
            }
            g2.setColor(color);
            g2.fillOval(sx - r, sy - r, r * 2, r * 2);

            if (p.isHomeworld()) {
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(sx - r - 2, sy - r - 2, (r + 2) * 2, (r + 2) * 2);
            }
            if (p == selected) {
                g2.setColor(Color.YELLOW);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(sx - r - 5, sy - r - 5, (r + 5) * 2, (r + 5) * 2);
            }

            g2.setColor(LABEL);
            g2.drawString(p.name(), sx + r + 3, sy + 4);
        }
    }

    private void drawFleets(Graphics2D g2) {
        for (Fleet f : galaxy.fleets()) {
            int sx = (int) toScreenX(f.x());
            int sy = (int) toScreenY(f.y());
            Player owner = galaxy.player(f.ownerId());
            Color c = owner != null ? new Color(owner.colorRgb()) : NEUTRAL;
            g2.setColor(c.brighter());
            int[] xs = { sx, sx - 5, sx + 5 };
            int[] ys = { sy - 6, sy + 5, sy + 5 };
            g2.fillPolygon(xs, ys, 3);
        }
    }

    private int planetRadius(Planet p) {
        if (!p.isColonized()) {
            return 3;
        }
        // Scale gently with population so busy worlds read as larger.
        int r = 4 + (int) Math.min(8, Math.log10(Math.max(10, p.population())));
        return r;
    }

    /** Find the planet whose screen position is nearest a click, within a radius. */
    private Planet planetAtScreen(int px, int py) {
        Planet best = null;
        double bestDist = 16; // px threshold
        List<Planet> planets = galaxy == null ? new ArrayList<Planet>() : galaxy.planets();
        for (Planet p : planets) {
            double dx = toScreenX(p.x()) - px;
            double dy = toScreenY(p.y()) - py;
            double d = Math.sqrt(dx * dx + dy * dy);
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    private final class MouseHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            lastDragX = e.getX();
            lastDragY = e.getY();
            dragging = false;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            dragging = true;
            offsetX += e.getX() - lastDragX;
            offsetY += e.getY() - lastDragY;
            lastDragX = e.getX();
            lastDragY = e.getY();
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (dragging) {
                return; // a drag, not a click
            }
            Planet p = planetAtScreen(e.getX(), e.getY());
            setSelected(p);
            if (selectionListener != null) {
                selectionListener.onPlanetSelected(p);
            }
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            double factor = e.getWheelRotation() < 0 ? 1.1 : 1 / 1.1;
            // Zoom toward the cursor.
            double wx = (e.getX() - offsetX) / scale;
            double wy = (e.getY() - offsetY) / scale;
            scale = Math.max(0.2, Math.min(4.0, scale * factor));
            offsetX = e.getX() - wx * scale;
            offsetY = e.getY() - wy * scale;
            repaint();
        }
    }
}
