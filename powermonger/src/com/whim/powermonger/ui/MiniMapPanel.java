package com.whim.powermonger.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JPanel;

import com.whim.powermonger.api.GameController;
import com.whim.powermonger.api.Views.CaptainView;
import com.whim.powermonger.api.Views.GameStateView;
import com.whim.powermonger.api.Views.TileView;
import com.whim.powermonger.api.Views.TownView;

/**
 * Scaled top-down map on the left. Terrain is drawn as flat colour cells; towns
 * and captains appear as dots. Clicking recenters the main isometric view on the
 * clicked tile.
 */
public final class MiniMapPanel extends JPanel {

    private final GameController controller;
    private final MapPanel mapPanel;
    private int cell = 5;
    private int ox, oy; // drawing origin inside the panel

    public MiniMapPanel(GameController controller, MapPanel mapPanel) {
        this.controller = controller;
        this.mapPanel = mapPanel;
        setBackground(UiPalette.PANEL_BG_DARK);
        setPreferredSize(new Dimension(180, 260));

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { recenter(e); }
        });
    }

    private void recenter(MouseEvent e) {
        GameStateView st = controller.state();
        double tx = (e.getX() - ox) / (double) cell;
        double ty = (e.getY() - oy) / (double) cell;
        tx = clamp(tx, 0, st.mapWidth() - 1);
        ty = clamp(ty, 0, st.mapHeight() - 1);
        mapPanel.centerOn(tx, ty);
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GameStateView st = controller.state();
        int w = st.mapWidth();
        int h = st.mapHeight();

        int avail = Math.min(getWidth() - 16, getHeight() - 16);
        cell = Math.max(2, avail / Math.max(w, h));
        int drawW = cell * w;
        int drawH = cell * h;
        ox = (getWidth() - drawW) / 2;
        oy = (getHeight() - drawH) / 2;

        // Terrain cells.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                TileView t = st.tile(x, y);
                if (t == null) continue;
                Color c = UiPalette.terrain(t.terrain());
                if (t.snowCovered()) c = UiPalette.SNOW;
                double lift = Math.min(0.3, t.elevation() * 0.05);
                g.setColor(UiPalette.lighten(c, lift));
                g.fillRect(ox + x * cell, oy + y * cell, cell, cell);
            }
        }

        // Towns.
        List<TownView> towns = st.towns();
        for (int i = 0; i < towns.size(); i++) {
            TownView tw = towns.get(i);
            g.setColor(UiPalette.faction(tw.allegiance()));
            int px = ox + tw.tileX() * cell;
            int py = oy + tw.tileY() * cell;
            g.fillRect(px - 1, py - 1, cell + 2, cell + 2);
            g.setColor(UiPalette.INK);
            g.drawRect(px - 1, py - 1, cell + 2, cell + 2);
        }

        // Captains.
        List<CaptainView> caps = st.captains();
        for (int i = 0; i < caps.size(); i++) {
            CaptainView c = caps.get(i);
            if (!c.alive()) continue;
            int px = ox + (int) Math.round(c.x() * cell);
            int py = oy + (int) Math.round(c.y() * cell);
            g.setColor(UiPalette.faction(c.allegiance()));
            g.fillOval(px - 3, py - 3, 6, 6);
            if (c.selected()) {
                g.setColor(UiPalette.HILIGHT);
                g.drawOval(px - 5, py - 5, 10, 10);
            }
        }

        // Camera reticle.
        g.setColor(UiPalette.HILIGHT);
        int camX = ox + (int) Math.round(mapPanel.renderer().camTileX() * cell);
        int camY = oy + (int) Math.round(mapPanel.renderer().camTileY() * cell);
        g.drawLine(camX - 5, camY, camX + 5, camY);
        g.drawLine(camX, camY - 5, camX, camY + 5);

        // Frame.
        g.setColor(UiPalette.PANEL_EDGE);
        g.drawRect(ox, oy, drawW, drawH);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
