package com.whim.startrek.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JPanel;

import com.whim.startrek.domain.Empire;
import com.whim.startrek.domain.Fleet;
import com.whim.startrek.domain.GalaxyMap;
import com.whim.startrek.domain.GameState;
import com.whim.startrek.domain.GridCell;
import com.whim.startrek.domain.MapObjectType;
import com.whim.startrek.domain.StarSystem;
import com.whim.startrek.engine.EconomyEngine;
import com.whim.startrek.engine.FleetAI;

/**
 * Macro TBS view: draws the {@link GalaxyMap} as a Graphics2D grid (terrain, star
 * systems, and stacked fleets) plus a lightweight minimap. Mouse handling:
 * <ul>
 *   <li>single-click a player fleet to select it, then single-click any cell to set
 *       its destination via {@link Fleet#setDestination(int, int)};</li>
 *   <li>double-click a star system to open the build menu + trade board.</li>
 * </ul>
 */
public class GalaxyPanel extends JPanel {

    private final GameState state;
    private final EconomyEngine economy;
    private final FleetAI fleetAI; // nullable; used to hide undetected cloaked enemies

    private Fleet selectedFleet;
    private int hoverRow = -1;
    private int hoverCol = -1;

    public GalaxyPanel(GameState state, EconomyEngine economy) {
        this(state, economy, null);
    }

    public GalaxyPanel(GameState state, EconomyEngine economy, FleetAI fleetAI) {
        this.state = state;
        this.economy = economy;
        this.fleetAI = fleetAI;
        setBackground(UiTheme.SPACE_BG);
        setPreferredSize(new Dimension(900, 720));
        MouseHandler mh = new MouseHandler();
        addMouseListener(mh);
        addMouseMotionListener(mh);
    }

    public Fleet getSelectedFleet() {
        return selectedFleet;
    }

    public void clearSelection() {
        selectedFleet = null;
        repaint();
    }

    // ---- layout helpers -------------------------------------------------

    private GalaxyMap map() {
        return state.getMap();
    }

    /** Square cell size that fits the whole grid in the current viewport. */
    private int cellSize() {
        GalaxyMap m = map();
        int reserveRight = 170; // minimap gutter
        int availW = Math.max(40, getWidth() - reserveRight - 16);
        int availH = Math.max(40, getHeight() - 16);
        int byW = availW / Math.max(1, m.getCols());
        int byH = availH / Math.max(1, m.getRows());
        return Math.max(14, Math.min(byW, byH));
    }

    private int originX() {
        return 8;
    }

    private int originY() {
        return 8;
    }

    private int cellAtX(int px) {
        int cs = cellSize();
        if (cs <= 0) {
            return -1;
        }
        int c = (px - originX()) / cs;
        return (c >= 0 && c < map().getCols()) ? c : -1;
    }

    private int cellAtY(int py) {
        int cs = cellSize();
        if (cs <= 0) {
            return -1;
        }
        int r = (py - originY()) / cs;
        return (r >= 0 && r < map().getRows()) ? r : -1;
    }

    // ---- rendering ------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            GalaxyMap m = map();
            int cs = cellSize();
            int ox = originX();
            int oy = originY();

            for (int r = 0; r < m.getRows(); r++) {
                for (int c = 0; c < m.getCols(); c++) {
                    drawCell(g2, m.getCell(r, c), ox + c * cs, oy + r * cs, cs);
                }
            }
            drawGridLines(g2, m, ox, oy, cs);
            drawSelection(g2, ox, oy, cs);
            drawMinimap(g2, m);
            drawHud(g2);
        } finally {
            g2.dispose();
        }
    }

    private void drawCell(Graphics2D g2, GridCell cell, int x, int y, int cs) {
        if (cell == null) {
            return;
        }
        MapObjectType t = cell.getType();
        g2.setColor(UiTheme.terrainColor(t));
        g2.fillRect(x, y, cs, cs);

        drawTerrainGlyph(g2, t, cell, x, y, cs);

        StarSystem sys = cell.getSystem();
        if (sys != null) {
            drawSystem(g2, sys, x, y, cs);
        }
        List<Fleet> fleets = cell.getFleets();
        if (fleets != null && !fleets.isEmpty()) {
            List<Fleet> visible = visibleFleets(fleets);
            if (!visible.isEmpty()) {
                drawFleetStack(g2, visible, x, y, cs);
            }
        }
    }

    /** Player fleets always show; enemy cloaked fleets only if our sensors detect them. */
    private List<Fleet> visibleFleets(List<Fleet> fleets) {
        Empire player = state.getPlayerEmpire();
        if (fleetAI == null || player == null) {
            return fleets;
        }
        List<Fleet> out = new java.util.ArrayList<Fleet>(fleets.size());
        for (Fleet f : fleets) {
            boolean mine = f.getOwner() == player.getRace();
            if (mine || !f.isCloaked() || fleetAI.isDetected(f, player, state)) {
                out.add(f);
            }
        }
        return out;
    }

    private void drawTerrainGlyph(Graphics2D g2, MapObjectType t, GridCell cell, int x, int y, int cs) {
        if (t == null) {
            return;
        }
        int cx = x + cs / 2;
        int cy = y + cs / 2;
        int rad = cs / 2 - 2;
        switch (t) {
            case BLACK_HOLE:
            case SUPER_BLACK_HOLE: {
                g2.setColor(new Color(0, 0, 0));
                g2.fillOval(cx - rad, cy - rad, rad * 2, rad * 2);
                g2.setColor(t == MapObjectType.SUPER_BLACK_HOLE
                        ? new Color(190, 90, 255) : new Color(90, 90, 130));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(cx - rad, cy - rad, rad * 2, rad * 2);
                break;
            }
            case SUPERNOVA: {
                g2.setColor(new Color(255, 180, 80));
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * i / 4.0;
                    g2.drawLine(cx, cy,
                            cx + (int) (Math.cos(a) * rad), cy + (int) (Math.sin(a) * rad));
                }
                break;
            }
            case ENERGY_STORM: {
                g2.setColor(new Color(120, 200, 255));
                g2.drawLine(x + 3, cy, cx, y + 3);
                g2.drawLine(cx, y + 3, cx, cy);
                g2.drawLine(cx, cy, x + cs - 3, cy + 3);
                break;
            }
            case NEBULA: {
                g2.setColor(new Color(150, 110, 200, 110));
                g2.fillOval(x + 2, y + 4, cs - 6, cs - 10);
                g2.fillOval(x + cs / 3, y + 2, cs - 8, cs - 6);
                break;
            }
            case STABLE_WORMHOLE:
            case UNSTABLE_WORMHOLE: {
                g2.setColor(t == MapObjectType.STABLE_WORMHOLE
                        ? new Color(90, 220, 210) : new Color(235, 170, 80));
                g2.setStroke(new BasicStroke(1.6f));
                for (int i = rad; i > 2; i -= 3) {
                    g2.drawOval(cx - i, cy - i, i * 2, i * 2);
                }
                break;
            }
            default:
                break;
        }
    }

    private void drawSystem(Graphics2D g2, StarSystem sys, int x, int y, int cs) {
        int cx = x + cs / 2;
        int cy = y + cs / 2;
        int rad = Math.max(3, cs / 4);
        g2.setColor(sys.isBorgControlled() ? new Color(120, 255, 120)
                : UiTheme.raceColor(sys.getOwner()));
        g2.fillOval(cx - rad, cy - rad, rad * 2, rad * 2);
        g2.setColor(new Color(255, 255, 255, 160));
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(cx - rad, cy - rad, rad * 2, rad * 2);
        if (cs >= 26) {
            g2.setColor(UiTheme.TEXT);
            g2.drawString(shorten(sys.getName(), 8), x + 2, y + cs - 3);
        }
    }

    private void drawFleetStack(Graphics2D g2, List<Fleet> fleets, int x, int y, int cs) {
        // Triangle marker in the top-right; stack count badge if more than one fleet.
        Fleet top = fleets.get(0);
        int s = Math.max(5, cs / 3);
        int px = x + cs - s - 3;
        int py = y + 3;
        g2.setColor(UiTheme.raceColor(top.getOwner()));
        int[] xs = { px, px + s, px + s / 2 };
        int[] ys = { py + s, py + s, py };
        g2.fillPolygon(xs, ys, 3);
        g2.setColor(top.isCloaked() ? new Color(180, 180, 255) : Color.WHITE);
        g2.drawPolygon(xs, ys, 3);
        if (fleets.size() > 1) {
            g2.setColor(UiTheme.SELECT);
            g2.fillOval(px - 6, py - 2, 11, 11);
            g2.setColor(Color.BLACK);
            g2.drawString(Integer.toString(fleets.size()), px - 3, py + 7);
        }
    }

    private void drawGridLines(Graphics2D g2, GalaxyMap m, int ox, int oy, int cs) {
        g2.setColor(UiTheme.GRID_LINE);
        g2.setStroke(new BasicStroke(1f));
        int w = m.getCols() * cs;
        int h = m.getRows() * cs;
        for (int c = 0; c <= m.getCols(); c++) {
            g2.drawLine(ox + c * cs, oy, ox + c * cs, oy + h);
        }
        for (int r = 0; r <= m.getRows(); r++) {
            g2.drawLine(ox, oy + r * cs, ox + w, oy + r * cs);
        }
        if (hoverRow >= 0 && hoverCol >= 0) {
            g2.setColor(new Color(255, 255, 255, 40));
            g2.fillRect(ox + hoverCol * cs, oy + hoverRow * cs, cs, cs);
        }
    }

    private void drawSelection(Graphics2D g2, int ox, int oy, int cs) {
        if (selectedFleet == null) {
            return;
        }
        int r = selectedFleet.getRow();
        int c = selectedFleet.getCol();
        g2.setColor(UiTheme.SELECT);
        g2.setStroke(new BasicStroke(2.4f));
        g2.drawRect(ox + c * cs + 1, oy + r * cs + 1, cs - 2, cs - 2);

        int dr = selectedFleet.getDestRow();
        int dc = selectedFleet.getDestCol();
        if (dr >= 0 && dc >= 0 && (dr != r || dc != c)) {
            g2.setColor(new Color(255, 214, 92, 180));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1f, new float[] { 6f, 6f }, 0f));
            g2.drawLine(ox + c * cs + cs / 2, oy + r * cs + cs / 2,
                    ox + dc * cs + cs / 2, oy + dr * cs + cs / 2);
        }
    }

    /** Minimap derived directly from the map array (terrain + system owners). */
    private void drawMinimap(Graphics2D g2, GalaxyMap m) {
        int pad = 8;
        int mmW = 150;
        int mmH = (int) ((double) mmW * m.getRows() / Math.max(1, m.getCols()));
        int mx = getWidth() - mmW - pad;
        int my = pad;
        g2.setColor(UiTheme.PANEL_BG);
        g2.fillRect(mx - 4, my - 4, mmW + 8, mmH + 8);
        g2.setColor(UiTheme.GRID_LINE);
        g2.drawRect(mx - 4, my - 4, mmW + 8, mmH + 8);

        double cw = (double) mmW / m.getCols();
        double ch = (double) mmH / m.getRows();
        for (int r = 0; r < m.getRows(); r++) {
            for (int c = 0; c < m.getCols(); c++) {
                GridCell cell = m.getCell(r, c);
                if (cell == null) {
                    continue;
                }
                Color col = UiTheme.terrainColor(cell.getType());
                if (cell.getSystem() != null) {
                    col = UiTheme.raceColor(cell.getSystem().getOwner());
                } else if (cell.getFleets() != null && !cell.getFleets().isEmpty()) {
                    col = UiTheme.raceColor(cell.getFleets().get(0).getOwner());
                }
                g2.setColor(col);
                g2.fillRect(mx + (int) (c * cw), my + (int) (r * ch),
                        (int) Math.ceil(cw), (int) Math.ceil(ch));
            }
        }
        if (selectedFleet != null) {
            g2.setColor(UiTheme.SELECT);
            g2.drawRect(mx + (int) (selectedFleet.getCol() * cw),
                    my + (int) (selectedFleet.getRow() * ch),
                    Math.max(2, (int) cw), Math.max(2, (int) ch));
        }
    }

    private void drawHud(Graphics2D g2) {
        g2.setColor(UiTheme.TEXT_DIM);
        String hint = selectedFleet == null
                ? "Click a fleet to select. Double-click a system for build/trade."
                : "Fleet #" + selectedFleet.getId() + " selected — click a cell to set destination.";
        g2.drawString(hint, 10, getHeight() - 8);
    }

    private static String shorten(String s, int n) {
        if (s == null) {
            return "";
        }
        return s.length() <= n ? s : s.substring(0, n - 1) + "…";
    }

    // ---- interaction ----------------------------------------------------

    private Fleet firstPlayerFleet(GridCell cell) {
        Empire player = state.getPlayerEmpire();
        if (cell == null || cell.getFleets() == null || player == null) {
            return null;
        }
        for (Fleet f : cell.getFleets()) {
            if (f.getOwner() == player.getRace()) {
                return f;
            }
        }
        return cell.getFleets().isEmpty() ? null : cell.getFleets().get(0);
    }

    private final class MouseHandler extends MouseAdapter {
        @Override
        public void mouseMoved(MouseEvent e) {
            int r = cellAtY(e.getY());
            int c = cellAtX(e.getX());
            if (r != hoverRow || c != hoverCol) {
                hoverRow = r;
                hoverCol = c;
                repaint();
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            int r = cellAtY(e.getY());
            int c = cellAtX(e.getX());
            if (r < 0 || c < 0) {
                return;
            }
            GridCell cell = map().getCell(r, c);
            if (cell == null) {
                return;
            }
            if (e.getClickCount() >= 2 && cell.getSystem() != null) {
                selectedFleet = null;
                new SystemDialog(GalaxyPanel.this, state, economy, cell.getSystem()).showDialog();
                repaint();
                return;
            }
            if (selectedFleet == null) {
                Fleet f = firstPlayerFleet(cell);
                if (f != null) {
                    selectedFleet = f;
                }
            } else {
                selectedFleet.setDestination(r, c);
                selectedFleet = null;
            }
            repaint();
        }
    }
}
