package com.whim.colony.ui;

import com.whim.colony.ColonyState;
import com.whim.colony.domain.Building;
import com.whim.colony.domain.BuildingType;
import com.whim.colony.domain.Colonist;
import com.whim.colony.domain.ColonyMap;
import com.whim.colony.domain.MapTile;
import com.whim.colony.domain.Needs;
import com.whim.colony.domain.TerrainType;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.List;

/**
 * READ-ONLY view of the {@link ColonyState} world. Renders the {@link ColonyMap}
 * grid as colored squares per {@link TerrainType}, draws {@link Building}s as
 * distinct shapes, and draws each {@link Colonist} as a colored oval with a tiny
 * needs/selection indicator.
 *
 * <p>The panel owns only <em>view</em> state: a camera offset (in tiles), a tile
 * size (zoom) and the currently selected colonist/tile. It never mutates domain
 * objects — every {@code paintComponent} pass simply reads whatever the current
 * state says. Frame rate is driven by {@link GameFrame}'s repaint timer and is
 * fully independent of the simulation tick.
 */
public class MapPanel extends JComponent {

    /** Default pixels-per-tile before any zoom. */
    public static final int DEFAULT_TILE_SIZE = 20;
    /** Zoom bounds (pixels-per-tile). */
    public static final int MIN_TILE_SIZE = 6;
    public static final int MAX_TILE_SIZE = 48;

    private ColonyState state;

    // ---- view (camera) state — NOT simulation state ----
    private int tileSize = DEFAULT_TILE_SIZE;
    /** Camera origin, in tile coordinates (top-left tile shown at 0,0 pixels). */
    private int cameraX = 0;
    private int cameraY = 0;

    // ---- selection (view-only) ----
    private Colonist selectedColonist; // nullable
    private int selectedTileX = -1;
    private int selectedTileY = -1;

    public MapPanel(ColonyState state) {
        this.state = state;
        setBackground(Color.BLACK);
        setOpaque(true);
        setPreferredSize(new Dimension(640, 640));
        setFocusable(true);
    }

    /** Swap in a new state to render (e.g. after the world is (re)built). */
    public void setState(ColonyState state) {
        this.state = state;
        this.selectedColonist = null;
        this.selectedTileX = -1;
        this.selectedTileY = -1;
        repaint();
    }

    public ColonyState getState() {
        return state;
    }

    // ------------------------------------------------------------------
    // Camera / zoom controls (called by GameFrame input handlers)
    // ------------------------------------------------------------------

    public int getTileSize() {
        return tileSize;
    }

    public void setTileSize(int tileSize) {
        this.tileSize = clamp(tileSize, MIN_TILE_SIZE, MAX_TILE_SIZE);
        repaint();
    }

    /** Zoom in/out by one step, keeping the camera origin fixed. */
    public void zoom(int deltaPixels) {
        setTileSize(tileSize + deltaPixels);
    }

    /** Pan the camera by whole tiles (positive dx moves the view right). */
    public void panTiles(int dx, int dy) {
        cameraX += dx;
        cameraY += dy;
        clampCamera();
        repaint();
    }

    public int getCameraX() {
        return cameraX;
    }

    public int getCameraY() {
        return cameraY;
    }

    private void clampCamera() {
        if (state == null || state.getMap() == null) {
            return;
        }
        ColonyMap map = state.getMap();
        int maxX = Math.max(0, map.getWidth() - 1);
        int maxY = Math.max(0, map.getHeight() - 1);
        cameraX = clamp(cameraX, 0, maxX);
        cameraY = clamp(cameraY, 0, maxY);
    }

    // ------------------------------------------------------------------
    // Selection (view-only) — populated by GameFrame's mouse handler
    // ------------------------------------------------------------------

    /** @return the tile X under the given pixel, or -1 if outside the map. */
    public int pixelToTileX(int px) {
        if (tileSize <= 0) {
            return -1;
        }
        return cameraX + (px / tileSize);
    }

    public int pixelToTileY(int py) {
        if (tileSize <= 0) {
            return -1;
        }
        return cameraY + (py / tileSize);
    }

    /**
     * Select whatever sits under the given pixel: a colonist if one stands on
     * that tile, otherwise the tile itself. Pure view state.
     *
     * @return the selected colonist, or {@code null} if only a tile was picked.
     */
    public Colonist selectAtPixel(int px, int py) {
        if (state == null || state.getMap() == null) {
            return null;
        }
        int tx = pixelToTileX(px);
        int ty = pixelToTileY(py);
        ColonyMap map = state.getMap();
        if (!map.inBounds(tx, ty)) {
            selectedColonist = null;
            selectedTileX = -1;
            selectedTileY = -1;
            repaint();
            return null;
        }
        selectedTileX = tx;
        selectedTileY = ty;
        selectedColonist = colonistAt(tx, ty);
        repaint();
        return selectedColonist;
    }

    private Colonist colonistAt(int tx, int ty) {
        List<Colonist> colonists = state.getColonists();
        for (int i = 0; i < colonists.size(); i++) {
            Colonist c = colonists.get(i);
            if (c.getX() == tx && c.getY() == ty) {
                return c;
            }
        }
        return null;
    }

    public Colonist getSelectedColonist() {
        return selectedColonist;
    }

    public int getSelectedTileX() {
        return selectedTileX;
    }

    public int getSelectedTileY() {
        return selectedTileY;
    }

    // ------------------------------------------------------------------
    // Rendering — READ ONLY over state
    // ------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (state == null || state.getMap() == null) {
                return;
            }
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            drawTiles(g2);
            drawColonists(g2);
            drawSelection(g2);
        } finally {
            g2.dispose();
        }
    }

    private void drawTiles(Graphics2D g2) {
        ColonyMap map = state.getMap();
        int cols = getWidth() / tileSize + 1;
        int rows = getHeight() / tileSize + 1;

        for (int sx = 0; sx < cols; sx++) {
            for (int sy = 0; sy < rows; sy++) {
                int tx = cameraX + sx;
                int ty = cameraY + sy;
                MapTile tile = map.getTile(tx, ty);
                if (tile == null) {
                    continue;
                }
                int px = sx * tileSize;
                int py = sy * tileSize;

                g2.setColor(terrainColor(tile.getTerrain()));
                g2.fillRect(px, py, tileSize, tileSize);

                if (tile.hasBuilding()) {
                    drawBuilding(g2, tile.getBuilding(), px, py);
                }

                // subtle grid line when tiles are large enough to matter
                if (tileSize >= 10) {
                    g2.setColor(new Color(0, 0, 0, 40));
                    g2.drawRect(px, py, tileSize, tileSize);
                }
            }
        }
    }

    private void drawBuilding(Graphics2D g2, Building building, int px, int py) {
        if (building == null) {
            return;
        }
        BuildingType type = building.getType();
        g2.setColor(buildingColor(type));
        int inset = Math.max(1, tileSize / 6);
        int size = tileSize - inset * 2;
        if (size < 1) {
            size = 1;
        }
        if (type == BuildingType.WALL) {
            // solid block, fills the tile
            g2.fillRect(px + 1, py + 1, tileSize - 2, tileSize - 2);
        } else if (type == BuildingType.BED) {
            // rounded rectangle
            g2.fillRoundRect(px + inset, py + inset, size, size, inset * 2, inset * 2);
        } else if (type == BuildingType.FARM) {
            // striped square to suggest rows
            g2.fillRect(px + inset, py + inset, size, size);
            g2.setColor(buildingColor(type).darker());
            int stripe = Math.max(1, size / 4);
            for (int i = px + inset; i < px + inset + size; i += stripe * 2) {
                g2.fillRect(i, py + inset, stripe, size);
            }
        } else {
            // STOCKPILE and anything else: outlined square
            g2.fillRect(px + inset, py + inset, size, size);
            g2.setColor(buildingColor(type).darker());
            g2.drawRect(px + inset, py + inset, size, size);
        }
    }

    private void drawColonists(Graphics2D g2) {
        ColonyMap map = state.getMap();
        List<Colonist> colonists = state.getColonists();
        for (int i = 0; i < colonists.size(); i++) {
            Colonist c = colonists.get(i);
            int sx = c.getX() - cameraX;
            int sy = c.getY() - cameraY;
            if (sx < 0 || sy < 0) {
                continue;
            }
            int px = sx * tileSize;
            int py = sy * tileSize;
            if (px > getWidth() || py > getHeight()) {
                continue;
            }
            if (!map.inBounds(c.getX(), c.getY())) {
                continue;
            }

            // planned path (thin trail) — read-only render of engine's path data
            drawPath(g2, c);

            int inset = Math.max(1, tileSize / 8);
            int size = tileSize - inset * 2;
            if (size < 3) {
                size = 3;
            }
            g2.setColor(moodColor(c.getNeeds()));
            g2.fillOval(px + inset, py + inset, size, size);
            g2.setColor(Color.BLACK);
            g2.drawOval(px + inset, py + inset, size, size);

            // tiny mood pip on top of the colonist
            drawNeedPip(g2, c.getNeeds(), px, py);
        }
    }

    private void drawPath(Graphics2D g2, Colonist c) {
        List<int[]> path = c.getPath();
        if (path == null || path.isEmpty() || tileSize < 8) {
            return;
        }
        g2.setColor(new Color(255, 255, 255, 90));
        int half = tileSize / 2;
        int prevX = c.getX();
        int prevY = c.getY();
        for (int i = 0; i < path.size(); i++) {
            int[] step = path.get(i);
            if (step == null || step.length < 2) {
                continue;
            }
            int ax = (prevX - cameraX) * tileSize + half;
            int ay = (prevY - cameraY) * tileSize + half;
            int bx = (step[0] - cameraX) * tileSize + half;
            int by = (step[1] - cameraY) * tileSize + half;
            g2.drawLine(ax, ay, bx, by);
            prevX = step[0];
            prevY = step[1];
        }
    }

    /** A small colored dot indicating the colonist's most pressing need. */
    private void drawNeedPip(Graphics2D g2, Needs needs, int px, int py) {
        if (tileSize < 12) {
            return;
        }
        double lowest = Math.min(needs.getHunger(), Math.min(needs.getRest(), needs.getMood()));
        if (lowest > Needs.LOW_THRESHOLD) {
            return; // nothing pressing to flag
        }
        Color pip = lowest <= Needs.CRITICAL_THRESHOLD ? Color.RED : Color.ORANGE;
        int r = Math.max(3, tileSize / 5);
        g2.setColor(pip);
        g2.fillOval(px + tileSize - r - 1, py + 1, r, r);
    }

    private void drawSelection(Graphics2D g2) {
        if (selectedTileX < 0 || selectedTileY < 0) {
            return;
        }
        int sx = selectedTileX - cameraX;
        int sy = selectedTileY - cameraY;
        if (sx < 0 || sy < 0) {
            return;
        }
        int px = sx * tileSize;
        int py = sy * tileSize;
        Stroke old = g2.getStroke();
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(Color.YELLOW);
        g2.drawRect(px + 1, py + 1, tileSize - 2, tileSize - 2);
        g2.setStroke(old);
    }

    // ------------------------------------------------------------------
    // Palette
    // ------------------------------------------------------------------

    private static Color terrainColor(TerrainType terrain) {
        if (terrain == null) {
            return Color.DARK_GRAY;
        }
        switch (terrain) {
            case GRASS:
                return new Color(76, 130, 66);
            case DIRT:
                return new Color(122, 96, 66);
            case ROCK:
                return new Color(110, 110, 118);
            case WATER:
                return new Color(48, 96, 160);
            case WALL:
                return new Color(70, 66, 60);
            default:
                return Color.DARK_GRAY;
        }
    }

    private static Color buildingColor(BuildingType type) {
        if (type == null) {
            return Color.LIGHT_GRAY;
        }
        switch (type) {
            case STOCKPILE:
                return new Color(200, 180, 90);
            case BED:
                return new Color(150, 110, 200);
            case FARM:
                return new Color(120, 190, 80);
            case WALL:
                return new Color(90, 84, 76);
            default:
                return Color.LIGHT_GRAY;
        }
    }

    /** Colonist body color shifts from green (content) to red (breaking) by mood. */
    private static Color moodColor(Needs needs) {
        if (needs == null) {
            return Color.WHITE;
        }
        double mood = needs.getMood();
        double t = clamp01(mood / Needs.MAX);
        int r = (int) (220 - 140 * t);
        int gr = (int) (80 + 150 * t);
        int b = 90;
        return new Color(clampByte(r), clampByte(gr), clampByte(b));
    }

    private static int clamp(int v, int lo, int hi) {
        if (v < lo) {
            return lo;
        }
        if (v > hi) {
            return hi;
        }
        return v;
    }

    private static double clamp01(double v) {
        if (v < 0) {
            return 0;
        }
        if (v > 1) {
            return 1;
        }
        return v;
    }

    private static int clampByte(int v) {
        return clamp(v, 0, 255);
    }
}
