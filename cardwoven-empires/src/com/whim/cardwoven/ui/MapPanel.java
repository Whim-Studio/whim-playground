package com.whim.cardwoven.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JPanel;

import com.whim.cardwoven.api.Enums.TerrainType;
import com.whim.cardwoven.api.GameController;
import com.whim.cardwoven.api.Views.AttachmentView;
import com.whim.cardwoven.api.Views.BuildingView;
import com.whim.cardwoven.api.Views.GameStateView;
import com.whim.cardwoven.api.Views.MapView;
import com.whim.cardwoven.api.Views.TileView;

/**
 * Renders the world grid with Graphics2D and handles click-to-select. Purely a
 * view: it reads {@code controller.state()} on every paint and reports tile
 * clicks to a {@link TileClickListener}. It never mutates game state itself.
 */
public class MapPanel extends JPanel {

    /** Notified when a map tile is clicked. */
    public interface TileClickListener {
        void onTileClicked(int row, int col);
    }

    private final GameController controller;
    private TileClickListener listener;

    private int selRow = -1, selCol = -1;
    // Geometry computed each paint so hit-testing matches what is drawn.
    private int cell = 48, ox = 0, oy = 0, rows = 0, cols = 0;

    public MapPanel(GameController controller) {
        this.controller = controller;
        setBackground(UiColors.MAP_BG);
        setPreferredSize(new Dimension(560, 460));
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { handleClick(e.getPoint()); }
        });
    }

    public void setTileClickListener(TileClickListener l) { this.listener = l; }

    public void setSelection(int row, int col) {
        this.selRow = row;
        this.selCol = col;
        repaint();
    }

    public int selectedRow() { return selRow; }
    public int selectedCol() { return selCol; }

    private void handleClick(Point p) {
        if (cols == 0 || rows == 0) return;
        int c = (p.x - ox) / cell;
        int r = (p.y - oy) / cell;
        if (r >= 0 && r < rows && c >= 0 && c < cols
                && p.x >= ox && p.y >= oy
                && p.x < ox + cols * cell && p.y < oy + rows * cell) {
            selRow = r;
            selCol = c;
            repaint();
            if (listener != null) listener.onTileClicked(r, c);
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        Renderer.hints(g);

        GameStateView state = controller.state();
        MapView map = state.map();
        rows = map.rows();
        cols = map.cols();
        if (rows == 0 || cols == 0) return;

        int w = getWidth(), h = getHeight();
        cell = Math.max(28, Math.min((w - 16) / cols, (h - 16) / rows));
        ox = (w - cell * cols) / 2;
        oy = (h - cell * rows) / 2;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                drawTile(g, map.tile(r, c), ox + c * cell, oy + r * cell, cell);
            }
        }

        // Selection highlight on top.
        if (selRow >= 0 && selCol >= 0 && selRow < rows && selCol < cols) {
            int x = ox + selCol * cell, y = oy + selRow * cell;
            g.setStroke(new BasicStroke(3f));
            g.setColor(UiColors.SELECT_GLOW);
            g.drawRoundRect(x + 1, y + 1, cell - 2, cell - 2, 8, 8);
            g.setColor(UiColors.withAlpha(UiColors.SELECT_GLOW, 40));
            g.fillRoundRect(x + 1, y + 1, cell - 2, cell - 2, 8, 8);
        }
    }

    private void drawTile(Graphics2D g, TileView t, int x, int y, int s) {
        TerrainType terrain = t.terrain();
        boolean explored = t.explored();

        Color base = UiColors.terrain(terrain);
        Color hi = UiColors.terrainHi(terrain);
        if (!explored) {
            base = UiColors.mix(base, UiColors.MAP_BG, 0.72);
            hi = UiColors.mix(hi, UiColors.MAP_BG, 0.72);
        }
        // Subtle vertical gradient per tile for depth.
        java.awt.GradientPaint gp = new java.awt.GradientPaint(x, y, hi, x, y + s, base);
        g.setPaint(gp);
        g.fillRect(x, y, s, s);

        // Terrain texture cues.
        drawTerrainDetail(g, terrain, x, y, s, explored);

        g.setColor(UiColors.GRID_LINE);
        g.setStroke(new BasicStroke(1f));
        g.drawRect(x, y, s, s);

        if (!explored) {
            g.setColor(UiColors.withAlpha(Color.BLACK, 70));
            g.fillRect(x, y, s, s);
            g.setColor(UiColors.TEXT_MUTED);
            g.setFont(g.getFont().deriveFont(Font.BOLD, s * 0.5f));
            g.drawString("?", x + s / 2 - s / 8, y + s / 2 + s / 6);
            return;
        }

        double cx = x + s / 2.0, cy = y + s / 2.0;

        BuildingView b = t.building();
        if (b != null) {
            Renderer.building(g, b.type(), cx, cy - s * 0.05, s * 0.28);
            drawAttachments(g, b.attachments(), x, y, s);
            // defense pip
            g.setColor(UiColors.withAlpha(Color.BLACK, 120));
            g.fillRoundRect(x + 3, y + s - 15, 22, 12, 6, 6);
            g.setColor(UiColors.TEXT);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 9f));
            g.drawString("❈" + b.defense(), x + 5, y + s - 5);
        }

        int rs = t.raiderStrength();
        if (rs > 0) {
            Renderer.raider(g, x + s * 0.78, y + s * 0.24, s * 0.12, rs);
        }
    }

    private void drawTerrainDetail(Graphics2D g, TerrainType terrain, int x, int y, int s,
                                   boolean explored) {
        Color ink = UiColors.withAlpha(Color.BLACK, explored ? 45 : 25);
        g.setStroke(new BasicStroke(Math.max(1f, s * 0.03f)));
        g.setColor(ink);
        if (terrain == TerrainType.WATER) {
            for (int i = 1; i <= 3; i++) {
                int wy = y + s * i / 4;
                g.drawArc(x + 4, wy - 4, s / 3, 8, 0, 180);
                g.drawArc(x + 4 + s / 2, wy - 4, s / 3, 8, 0, 180);
            }
        } else if (terrain == TerrainType.FOREST) {
            for (int i = 0; i < 3; i++) {
                int tx = x + s / 6 + (i * s) / 4;
                int ty = y + s / 2 + (i % 2) * s / 6;
                java.awt.Polygon tree = new java.awt.Polygon();
                tree.addPoint(tx, ty - s / 6);
                tree.addPoint(tx - s / 12, ty + s / 12);
                tree.addPoint(tx + s / 12, ty + s / 12);
                g.fill(tree);
            }
        } else if (terrain == TerrainType.MOUNTAIN) {
            java.awt.Polygon m = new java.awt.Polygon();
            m.addPoint(x + s / 6, y + s * 3 / 4);
            m.addPoint(x + s / 2, y + s / 4);
            m.addPoint(x + s * 5 / 6, y + s * 3 / 4);
            g.draw(m);
        } else if (terrain == TerrainType.DESERT) {
            for (int i = 0; i < 4; i++) {
                int dx = x + 4 + (i * (s - 8)) / 4;
                g.fillOval(dx, y + s / 2 + (i % 2) * s / 6, 3, 3);
            }
        } else { // PLAINS
            for (int i = 0; i < 5; i++) {
                int gx = x + 4 + (i * (s - 8)) / 5;
                g.drawLine(gx, y + s - 6, gx, y + s - 6 - s / 8);
            }
        }
    }

    private void drawAttachments(Graphics2D g, List<AttachmentView> atts, int x, int y, int s) {
        if (atts == null || atts.isEmpty()) return;
        double gr = s * 0.09;
        double gx = x + s * 0.20;
        double gy = y + s * 0.80;
        int i = 0;
        for (AttachmentView a : atts) {
            Renderer.attachment(g, a.type(), gx + i * (gr * 2.4), gy, gr);
            i++;
            if (i >= 3) break; // keep it tidy
        }
    }
}
