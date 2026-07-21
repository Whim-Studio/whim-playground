package com.railroad.ui;

import com.railroad.model.GameState;
import com.railroad.model.GridPoint;
import com.railroad.model.Industry;
import com.railroad.model.Route;
import com.railroad.model.Station;
import com.railroad.model.TerrainType;
import com.railroad.model.TileGrid;
import com.railroad.model.Town;
import com.railroad.model.Train;
import com.railroad.model.TrackSegment;
import com.railroad.model.World;

import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * The scrollable map view. Draws the terrain grid in programmatic colours, towns
 * as labelled markers, laid track as lines, and the train as a moving marker.
 * A legend is painted anchored to the visible viewport so it stays on screen
 * while the map is panned inside its {@link javax.swing.JScrollPane}.
 *
 * <p>When the Build Track tool is active, pressing and dragging across adjacent
 * tiles lays track segment by segment via the {@link GameController}.
 */
public final class MapPanel extends JPanel {

    private static final int TILE = 22;
    private static final Color GRID_LINE = new Color(0, 0, 0, 40);
    private static final Color TRACK_COLOR = new Color(30, 30, 30);
    private static final Color TRAIN_COLOR = new Color(200, 40, 40);
    private static final Color TOWN_FILL = new Color(250, 240, 180);
    private static final Color TOWN_EDGE = new Color(60, 40, 10);
    private static final Color STATION_FILL = new Color(240, 60, 60);
    private static final Color STATION_EDGE = new Color(255, 255, 255);
    private static final Color CATCHMENT_FILL = new Color(120, 200, 255, 55);
    private static final Color CATCHMENT_EDGE = new Color(120, 200, 255, 130);

    private final GameController controller;
    private final Runnable onChange; // refresh HUD after a build action

    private GridPoint lastDragTile; // last tile touched during a build drag

    public MapPanel(GameController controller, Runnable onChange) {
        this.controller = controller;
        this.onChange = onChange;
        World world = controller.getState().getWorld();
        TileGrid grid = world.getGrid();
        setPreferredSize(new Dimension(grid.getWidth() * TILE, grid.getHeight() * TILE));
        setBackground(new Color(20, 20, 30));

        MouseInputAdapter mouse = new MouseInputAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (controller.getCurrentTool() == Tool.BUILD_TRACK) {
                    lastDragTile = tileAtPixel(e.getX(), e.getY());
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (controller.getCurrentTool() != Tool.BUILD_TRACK) {
                    return;
                }
                GridPoint cur = tileAtPixel(e.getX(), e.getY());
                if (cur == null || cur.equals(lastDragTile)) {
                    return;
                }
                if (lastDragTile != null && lastDragTile.isAdjacent(cur)) {
                    controller.tryLayTrack(lastDragTile, cur);
                    repaint();
                    if (onChange != null) {
                        onChange.run();
                    }
                }
                lastDragTile = cur;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                lastDragTile = null;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                GridPoint p = tileAtPixel(e.getX(), e.getY());
                if (p == null) {
                    return;
                }
                if (controller.getCurrentTool() == Tool.BUILD_STATION) {
                    controller.tryBuildStation(p);
                    repaint();
                    if (onChange != null) {
                        onChange.run();
                    }
                } else if (controller.getCurrentTool() == Tool.SELECT) {
                    controller.selectTrainNear(p);
                    if (onChange != null) {
                        onChange.run();
                    }
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    private GridPoint tileAtPixel(int px, int py) {
        int tx = px / TILE;
        int ty = py / TILE;
        TileGrid grid = controller.getState().getWorld().getGrid();
        if (!grid.inBounds(tx, ty)) {
            return null;
        }
        return new GridPoint(tx, ty);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        GameState state = controller.getState();
        World world = state.getWorld();
        drawTerrain(g2, world.getGrid());
        drawCatchments(g2, state);
        drawTrack(g2, state);
        drawIndustries(g2, world.getIndustries());
        drawTowns(g2, world.getTowns());
        drawStations(g2, state);
        drawTrains(g2, state);
        drawLegend(g2);
    }

    /** Translucent overlay of every station's catchment tiles. */
    private void drawCatchments(Graphics2D g2, GameState state) {
        for (Station s : state.getStations()) {
            for (GridPoint p : s.getCatchment()) {
                g2.setColor(CATCHMENT_FILL);
                g2.fillRect(p.x * TILE, p.y * TILE, TILE, TILE);
                g2.setColor(CATCHMENT_EDGE);
                g2.drawRect(p.x * TILE, p.y * TILE, TILE, TILE);
            }
        }
    }

    /** Industries as distinct square markers with a one-letter tag and label. */
    private void drawIndustries(Graphics2D g2, List<Industry> industries) {
        for (Industry ind : industries) {
            int cx = center(ind.getPosition().x);
            int cy = center(ind.getPosition().y);
            int s = TILE - 8;
            g2.setColor(ind.getType().getColor());
            g2.fillRect(cx - s / 2, cy - s / 2, s, s);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(cx - s / 2, cy - s / 2, s, s);

            String tag = ind.getType() == com.railroad.model.IndustryType.COAL_MINE ? "C" : "S";
            int tw = g2.getFontMetrics().stringWidth(tag);
            g2.drawString(tag, cx - tw / 2, cy + 4);

            String name = ind.getName();
            int nw = g2.getFontMetrics().stringWidth(name);
            int lx = cx - nw / 2;
            int ly = cy + s / 2 + 12;
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRect(lx - 2, ly - g2.getFontMetrics().getAscent(), nw + 4,
                    g2.getFontMetrics().getHeight());
            g2.setColor(Color.WHITE);
            g2.drawString(name, lx, ly);
        }
    }

    /** Stations as small red diamonds sitting on their tile. */
    private void drawStations(Graphics2D g2, GameState state) {
        for (Station s : state.getStations()) {
            int cx = center(s.getPosition().x);
            int cy = center(s.getPosition().y);
            int r = TILE / 2 - 2;
            int[] xs = {cx, cx + r, cx, cx - r};
            int[] ys = {cy - r, cy, cy + r, cy};
            g2.setColor(STATION_FILL);
            g2.fillPolygon(xs, ys, 4);
            g2.setColor(STATION_EDGE);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawPolygon(xs, ys, 4);
        }
    }

    private void drawTerrain(Graphics2D g2, TileGrid grid) {
        Rectangle clip = g2.getClipBounds();
        int minX = Math.max(0, clip.x / TILE);
        int minY = Math.max(0, clip.y / TILE);
        int maxX = Math.min(grid.getWidth(), (clip.x + clip.width) / TILE + 1);
        int maxY = Math.min(grid.getHeight(), (clip.y + clip.height) / TILE + 1);
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                g2.setColor(grid.terrainAt(x, y).getColor());
                g2.fillRect(x * TILE, y * TILE, TILE, TILE);
                g2.setColor(GRID_LINE);
                g2.drawRect(x * TILE, y * TILE, TILE, TILE);
            }
        }
    }

    private void drawTrack(Graphics2D g2, GameState state) {
        g2.setColor(TRACK_COLOR);
        g2.setStroke(new BasicStroke(3f));
        for (TrackSegment seg : state.getNetwork().getSegments()) {
            int ax = center(seg.getA().x);
            int ay = center(seg.getA().y);
            int bx = center(seg.getB().x);
            int by = center(seg.getB().y);
            g2.drawLine(ax, ay, bx, by);
        }
    }

    private void drawTowns(Graphics2D g2, List<Town> towns) {
        for (Town t : towns) {
            int cx = center(t.getX());
            int cy = center(t.getY());
            int r = TILE - 6;
            g2.setColor(TOWN_FILL);
            g2.fillOval(cx - r / 2, cy - r / 2, r, r);
            g2.setColor(TOWN_EDGE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(cx - r / 2, cy - r / 2, r, r);

            String name = t.getName();
            int tw = g2.getFontMetrics().stringWidth(name);
            int lx = cx - tw / 2;
            int ly = cy - r / 2 - 4;
            g2.setColor(new Color(255, 255, 255, 210));
            g2.fillRect(lx - 2, ly - g2.getFontMetrics().getAscent(), tw + 4,
                    g2.getFontMetrics().getHeight());
            g2.setColor(Color.BLACK);
            g2.drawString(name, lx, ly);
        }
    }

    private void drawTrains(Graphics2D g2, GameState state) {
        for (Train train : state.getTrains()) {
            Route route = train.getRoute();
            List<GridPoint> path = route.getPath();
            double pos = train.getPosition();
            int i = (int) Math.floor(pos);
            if (i >= path.size() - 1) {
                i = path.size() - 2;
            }
            if (i < 0) {
                i = 0;
            }
            double frac = pos - i;
            GridPoint a = path.get(i);
            GridPoint b = path.get(i + 1);
            int px = (int) (center(a.x) + (center(b.x) - center(a.x)) * frac);
            int py = (int) (center(a.y) + (center(b.y) - center(a.y)) * frac);
            int s = TILE - 8;
            g2.setColor(TRAIN_COLOR);
            g2.fillRect(px - s / 2, py - s / 2, s, s);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRect(px - s / 2, py - s / 2, s, s);

            // Small load indicator above the train (carloads / capacity).
            String load = train.loadCount() + "/" + train.getCapacity();
            int lw = g2.getFontMetrics().stringWidth(load);
            int lx = px - lw / 2;
            int ly = py - s / 2 - 3;
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(lx - 2, ly - g2.getFontMetrics().getAscent(), lw + 4,
                    g2.getFontMetrics().getHeight());
            g2.setColor(Color.WHITE);
            g2.drawString(load, lx, ly);
        }
    }

    /** Legend anchored to the current viewport corner so it stays visible. */
    private void drawLegend(Graphics2D g2) {
        Rectangle view = getVisibleRect();
        int pad = 8;
        int rowH = 18;
        TerrainType[] types = TerrainType.values();
        int boxW = 150;
        int boxH = pad * 2 + rowH * (types.length + 1);
        int x = view.x + 10;
        int y = view.y + 10;

        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRoundRect(x, y, boxW, boxH, 10, 10);
        g2.setColor(Color.WHITE);
        g2.drawString("Terrain (segment cost)", x + pad, y + pad + 12);
        int ry = y + pad + rowH + 6;
        for (TerrainType t : types) {
            g2.setColor(t.getColor());
            g2.fillRect(x + pad, ry - 10, 12, 12);
            g2.setColor(Color.WHITE);
            g2.drawRect(x + pad, ry - 10, 12, 12);
            g2.drawString(t.getLabel() + "  $" + t.getSegmentCost(), x + pad + 20, ry);
            ry += rowH;
        }

        // Second panel: Phase 2 map symbols.
        int y2 = y + boxH + 8;
        int boxH2 = pad * 2 + rowH * 4;
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRoundRect(x, y2, boxW, boxH2, 10, 10);
        g2.setColor(Color.WHITE);
        g2.drawString("Map symbols", x + pad, y2 + pad + 12);
        int sy = y2 + pad + rowH + 6;

        // Station diamond swatch.
        int sc = x + pad + 6;
        int[] dx = {sc, sc + 6, sc, sc - 6};
        int[] dy = {sy - 12, sy - 6, sy, sy - 6};
        g2.setColor(STATION_FILL);
        g2.fillPolygon(dx, dy, 4);
        g2.setColor(Color.WHITE);
        g2.drawString("Station (catchment ring)", x + pad + 20, sy);
        sy += rowH;

        g2.setColor(com.railroad.model.IndustryType.COAL_MINE.getColor());
        g2.fillRect(x + pad, sy - 11, 12, 12);
        g2.setColor(Color.WHITE);
        g2.drawRect(x + pad, sy - 11, 12, 12);
        g2.drawString("C = Coal Mine (coal)", x + pad + 20, sy);
        sy += rowH;

        g2.setColor(com.railroad.model.IndustryType.STEEL_MILL.getColor());
        g2.fillRect(x + pad, sy - 11, 12, 12);
        g2.setColor(Color.WHITE);
        g2.drawRect(x + pad, sy - 11, 12, 12);
        g2.drawString("S = Steel Mill (coal->steel)", x + pad + 20, sy);
    }

    private static int center(int tileCoord) {
        return tileCoord * TILE + TILE / 2;
    }
}
