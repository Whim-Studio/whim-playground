package com.whim.civ.ui;

import com.whim.civ.domain.City;
import com.whim.civ.domain.GameMap;
import com.whim.civ.domain.GameState;
import com.whim.civ.domain.Improvement;
import com.whim.civ.domain.Tile;
import com.whim.civ.domain.Unit;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Top-down, <b>square-tiled</b> world viewport (never hexes). Renders terrain by colour,
 * Settler improvements (road / railroad / irrigation / mine), goody huts, cities, and the
 * active civ's units. Designed to live inside a {@code JScrollPane}; it implements
 * {@link Scrollable} so {@link #centerOn(int, int)} can jump to any tile and the wheel/bars
 * scroll tile-by-tile. A single left click reports the tile to a {@link TileListener}.
 */
public final class MapPanel extends JPanel implements Scrollable {

    /** Click callback: the host decides whether the tile selects a unit, opens a city, or moves. */
    public interface TileListener {
        void tileClicked(int tileX, int tileY);
    }

    private final GameState state;
    private int tileSize = 28;
    private TileListener listener;
    private Unit selected;
    private int hoverX = -1;
    private int hoverY = -1;

    public MapPanel(GameState state) {
        this.state = state;
        setBackground(new Color(20, 30, 50));
        setOpaque(true);
        setFocusable(true);
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                int tx = e.getX() / tileSize;
                int ty = e.getY() / tileSize;
                if (state.getMap().inBounds(tx, ty) && listener != null) {
                    listener.tileClicked(tx, ty);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int tx = e.getX() / tileSize;
                int ty = e.getY() / tileSize;
                if (tx != hoverX || ty != hoverY) {
                    hoverX = tx;
                    hoverY = ty;
                    repaint();
                }
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    public void setTileListener(TileListener l) {
        this.listener = l;
    }

    public void setSelectedUnit(Unit u) {
        this.selected = u;
        repaint();
    }

    public void setTileSize(int px) {
        this.tileSize = Math.max(8, px);
        revalidate();
        repaint();
    }

    public int getTileSize() {
        return tileSize;
    }

    /** Jump-to-coordinate centering: scrolls so the given tile sits in the viewport middle. */
    public void centerOn(int tileX, int tileY) {
        int cx = tileX * tileSize + tileSize / 2;
        int cy = tileY * tileSize + tileSize / 2;
        Rectangle view = getVisibleRect();
        int x = cx - view.width / 2;
        int y = cy - view.height / 2;
        scrollRectToVisible(new Rectangle(x, y, view.width, view.height));
    }

    @Override
    public Dimension getPreferredSize() {
        GameMap m = state.getMap();
        return new Dimension(m.getWidth() * tileSize, m.getHeight() * tileSize);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        GameMap map = state.getMap();
        Rectangle clip = g2.getClipBounds();
        int x0 = Math.max(0, clip.x / tileSize);
        int y0 = Math.max(0, clip.y / tileSize);
        int x1 = Math.min(map.getWidth() - 1, (clip.x + clip.width) / tileSize);
        int y1 = Math.min(map.getHeight() - 1, (clip.y + clip.height) / tileSize);

        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                paintTile(g2, map.getTile(x, y), x, y);
            }
        }

        // Cities and units sit above the terrain layer.
        for (City c : state.getCities()) {
            paintCity(g2, c);
        }
        for (Unit u : state.getUnits()) {
            if (u.isAlive() && u.getOwnerCivId() == state.getActiveCivIndex()) {
                paintUnit(g2, u);
            }
        }

        // Selection ring.
        if (selected != null && selected.isAlive()) {
            int px = selected.getX() * tileSize;
            int py = selected.getY() * tileSize;
            g2.setColor(UiTheme.SELECTION);
            g2.drawRect(px + 1, py + 1, tileSize - 3, tileSize - 3);
            g2.drawRect(px + 2, py + 2, tileSize - 5, tileSize - 5);
        }
        if (map.inBounds(hoverX, hoverY)) {
            g2.setColor(new Color(255, 255, 255, 70));
            g2.drawRect(hoverX * tileSize, hoverY * tileSize, tileSize - 1, tileSize - 1);
        }
        g2.dispose();
    }

    private void paintTile(Graphics2D g2, Tile t, int x, int y) {
        int px = x * tileSize;
        int py = y * tileSize;
        g2.setColor(UiTheme.terrainColor(t.getTerrain()));
        g2.fillRect(px, py, tileSize, tileSize);

        Improvement imp = t.getImprovement();
        if (imp == Improvement.IRRIGATION) {
            paintIrrigation(g2, px, py);
        } else if (imp == Improvement.MINE) {
            paintMine(g2, px, py);
        }
        if (t.hasRoad()) {
            g2.setColor(imp == Improvement.RAILROAD ? UiTheme.RAILROAD : UiTheme.ROAD);
            g2.fillRect(px + tileSize / 2 - 1, py, 2, tileSize);
            g2.fillRect(px, py + tileSize / 2 - 1, tileSize, 2);
        }
        if (t.hasGoodyHut()) {
            int s = Math.max(6, tileSize / 3);
            g2.setColor(UiTheme.GOODY);
            g2.fillRect(px + tileSize / 2 - s / 2, py + tileSize / 2 - s / 2, s, s);
            g2.setColor(Color.BLACK);
            g2.drawRect(px + tileSize / 2 - s / 2, py + tileSize / 2 - s / 2, s, s);
        }
        g2.setColor(UiTheme.GRID);
        g2.drawRect(px, py, tileSize, tileSize);
    }

    private void paintIrrigation(Graphics2D g2, int px, int py) {
        g2.setColor(new Color(80, 150, 230, 160));
        int step = Math.max(4, tileSize / 4);
        for (int i = step / 2; i < tileSize; i += step) {
            g2.drawLine(px + 2, py + i, px + tileSize - 2, py + i);
        }
    }

    private void paintMine(Graphics2D g2, int px, int py) {
        g2.setColor(new Color(60, 50, 40));
        int s = Math.max(3, tileSize / 5);
        g2.fillRect(px + tileSize - s - 2, py + 2, s, s);
        g2.fillRect(px + tileSize - 2 * s - 4, py + 2, s, s);
    }

    private void paintCity(Graphics2D g2, City c) {
        int px = c.getX() * tileSize;
        int py = c.getY() * tileSize;
        g2.setColor(UiTheme.civColor(c.getOwnerCivId()));
        g2.fillRect(px + 2, py + 2, tileSize - 4, tileSize - 4);
        g2.setColor(c.isInDisorder() ? Color.RED : Color.WHITE);
        g2.drawRect(px + 2, py + 2, tileSize - 5, tileSize - 5);
        // Population badge.
        String pop = Integer.toString(c.getPopulation());
        g2.setFont(UiTheme.H2);
        g2.setColor(Color.BLACK);
        g2.drawString(pop, px + 5, py + tileSize - 5);
        g2.setColor(Color.WHITE);
        g2.drawString(pop, px + 4, py + tileSize - 6);
    }

    private void paintUnit(Graphics2D g2, Unit u) {
        int px = u.getX() * tileSize;
        int py = u.getY() * tileSize;
        int pad = Math.max(3, tileSize / 5);
        g2.setColor(UiTheme.civColor(u.getOwnerCivId()));
        g2.fillOval(px + pad, py + pad, tileSize - 2 * pad, tileSize - 2 * pad);
        g2.setColor(Color.BLACK);
        g2.drawOval(px + pad, py + pad, tileSize - 2 * pad, tileSize - 2 * pad);
        // Unit type initial.
        String letter = u.getType().name().substring(0, 1);
        g2.setFont(UiTheme.BODY);
        g2.setColor(Color.WHITE);
        g2.drawString(letter, px + tileSize / 2 - 4, py + tileSize / 2 + 4);
        if (u.isFortified()) {
            g2.setColor(Color.WHITE);
            g2.drawRect(px + pad - 2, py + pad - 2, tileSize - 2 * pad + 4, tileSize - 2 * pad + 4);
        }
    }

    // ---- Scrollable: tile-granular scrolling inside a JScrollPane ----

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(Math.min(getPreferredSize().width, 18 * tileSize),
                Math.min(getPreferredSize().height, 14 * tileSize));
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return tileSize;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == SwingConstants.HORIZONTAL ? visibleRect.width : visibleRect.height;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
