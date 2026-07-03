package com.whim.populous.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JPanel;

import com.whim.populous.api.ActionResult;
import com.whim.populous.api.Enums.GodPower;
import com.whim.populous.api.GameController;
import com.whim.populous.api.Views.FollowerView;
import com.whim.populous.api.Views.GameStateView;
import com.whim.populous.api.Views.MapView;
import com.whim.populous.api.Views.TileView;

/**
 * Paints the terrain + entities each frame and translates mouse input into
 * {@link GameController} calls. Strict top-down grid; pixel size adapts to the
 * panel so the whole 64x64 map is always visible.
 *
 * Input mapping (classic Populous):
 *   LEFT click / drag  -> {@link GameController#primaryClick} (raise land or
 *                         cast the armed targeted power)
 *   RIGHT click / drag -> {@link GameController#secondaryClick} (lower land)
 */
public class MapPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final GameController controller;

    // Layout computed each paint so resizing just works.
    private int tileSize = 10;
    private int originX = 0;
    private int originY = 0;

    // Hover + last dragged cell (to avoid re-firing on the same tile mid-drag).
    private int hoverCol = -1;
    private int hoverRow = -1;
    private int lastCol = -1;
    private int lastRow = -1;
    private String flash = "";

    public MapPanel(GameController controller) {
        this.controller = controller;
        setBackground(UiColors.WATER_DEEP);
        setPreferredSize(new Dimension(640, 640));
        MouseHandler h = new MouseHandler();
        addMouseListener(h);
        addMouseMotionListener(h);
    }

    private void recomputeLayout() {
        MapView map = controller.state().map();
        int cols = Math.max(1, map.cols());
        int rows = Math.max(1, map.rows());
        int ts = Math.max(2, Math.min(getWidth() / cols, getHeight() / rows));
        this.tileSize = ts;
        this.originX = (getWidth() - ts * cols) / 2;
        this.originY = (getHeight() - ts * rows) / 2;
    }

    /** Pixel -> tile column, or -1 if outside the map. */
    private int toCol(int px) {
        MapView map = controller.state().map();
        int c = (px - originX) / tileSize;
        return (c >= 0 && c < map.cols()) ? c : -1;
    }

    private int toRow(int py) {
        MapView map = controller.state().map();
        int r = (py - originY) / tileSize;
        return (r >= 0 && r < map.rows()) ? r : -1;
    }

    @Override
    protected void paintComponent(Graphics gRaw) {
        super.paintComponent(gRaw);
        Graphics2D g = (Graphics2D) gRaw;
        Renderer.hints(g);
        recomputeLayout();

        GameStateView st = controller.state();
        MapView map = st.map();
        int ts = tileSize;

        // --- terrain pass ---
        for (int row = 0; row < map.rows(); row++) {
            int py = originY + row * ts;
            for (int col = 0; col < map.cols(); col++) {
                int px = originX + col * ts;
                TileView t = map.tileAt(col, row);
                Renderer.drawTile(g, t, px, py, ts, map.seaLevel());
                if (ts >= 6) {
                    Renderer.drawContour(g, map, col, row, px, py, ts);
                }
            }
        }

        // --- settlement pass (on top of terrain) ---
        for (int row = 0; row < map.rows(); row++) {
            for (int col = 0; col < map.cols(); col++) {
                TileView t = map.tileAt(col, row);
                if (t.settlement() != null && !"NONE".equals(t.settlement().name())) {
                    Renderer.drawSettlement(g, t.settlement(), t.owner(),
                            originX + col * ts, originY + row * ts, ts);
                }
            }
        }

        // --- papal magnets ---
        drawMagnet(g, st, ts, true);
        drawMagnet(g, st, ts, false);

        // --- followers ---
        List<FollowerView> followers = st.followers();
        int dot = Math.max(3, ts / 2);
        for (int i = 0; i < followers.size(); i++) {
            FollowerView f = followers.get(i);
            if (!f.alive()) {
                continue;
            }
            int px = originX + (int) Math.round(f.x() * ts) + ts / 2;
            int py = originY + (int) Math.round(f.y() * ts) + ts / 2;
            Renderer.drawFollower(g, f, px, py, dot);
        }

        // --- hover highlight + armed-power cursor ---
        if (hoverCol >= 0 && hoverRow >= 0) {
            int px = originX + hoverCol * ts;
            int py = originY + hoverRow * ts;
            GodPower armed = st.selectedPower();
            Color ring = (armed != null && st.powerAffordable(armed))
                    ? UiColors.HIGHLIGHT : UiColors.HUD_TEXT_DIM;
            g.setColor(ring);
            g.drawRect(px, py, ts - 1, ts - 1);
        }

        // --- flash message from last action ---
        if (flash != null && flash.length() > 0) {
            g.setColor(UiColors.withAlpha(Color.BLACK, 150));
            g.fillRect(6, 6, 8 + flash.length() * 7, 20);
            g.setColor(UiColors.HUD_TEXT);
            g.drawString(flash, 12, 20);
        }
    }

    private void drawMagnet(Graphics2D g, GameStateView st, int ts, boolean good) {
        com.whim.populous.api.Views.PapalMagnetView m =
                good ? st.goodMagnet() : st.evilMagnet();
        if (m != null && m.active()) {
            Renderer.drawMagnet(g, m, originX + m.col() * ts, originY + m.row() * ts, ts);
        }
    }

    private void fireAt(int col, int row, boolean primary) {
        if (col < 0 || row < 0) {
            return;
        }
        if (col == lastCol && row == lastRow) {
            return; // already acted on this tile during the drag
        }
        lastCol = col;
        lastRow = row;
        ActionResult r = primary ? controller.primaryClick(col, row)
                                  : controller.secondaryClick(col, row);
        if (r != null && r.message() != null) {
            flash = r.message();
        }
        repaint();
    }

    private class MouseHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            lastCol = -1;
            lastRow = -1;
            handle(e);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            hoverCol = toCol(e.getX());
            hoverRow = toRow(e.getY());
            handle(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            hoverCol = toCol(e.getX());
            hoverRow = toRow(e.getY());
            repaint();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            hoverCol = -1;
            hoverRow = -1;
            repaint();
        }

        private void handle(MouseEvent e) {
            int col = toCol(e.getX());
            int row = toRow(e.getY());
            boolean primary = !(javax.swing.SwingUtilities.isRightMouseButton(e)
                    || e.isMetaDown() || e.isControlDown());
            fireAt(col, row, primary);
        }
    }
}
