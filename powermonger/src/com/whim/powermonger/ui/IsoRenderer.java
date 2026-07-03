package com.whim.powermonger.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Point2D;

import com.whim.powermonger.api.Enums.TerrainType;
import com.whim.powermonger.api.Views.GameStateView;
import com.whim.powermonger.api.Views.TileView;

/**
 * Pseudo-isometric (2.5D) projection and terrain painter. Projects a tile at
 * fractional map coordinates plus an elevation band to screen space, and draws
 * the diamond terrain top with an elevation "lift", a vertical earth wall and a
 * drop-shadow so the topography reads at a glance.
 *
 * <p>Purely {@code Graphics2D} — no assets. Reads only {@code Views}.</p>
 */
public final class IsoRenderer {

    /** Half-width / half-height of a tile diamond in screen pixels. */
    public static final int TILE_HW = 26;
    public static final int TILE_HH = 14;
    /** Screen pixels a tile rises per elevation band. */
    public static final int ELEV_LIFT = 11;

    /** Camera centre in tile coordinates and screen viewport centre. */
    private double camTileX;
    private double camTileY;
    private int viewW;
    private int viewH;

    public IsoRenderer() {
        this.camTileX = 0;
        this.camTileY = 0;
    }

    public void setViewport(int w, int h) {
        this.viewW = w;
        this.viewH = h;
    }

    /** Centre the camera on a tile position. */
    public void centerOn(double tileX, double tileY) {
        this.camTileX = tileX;
        this.camTileY = tileY;
    }

    public double camTileX() { return camTileX; }
    public double camTileY() { return camTileY; }

    /**
     * Project a map position (fractional tiles) at a given elevation band to a
     * screen point. Elevation raises the point on screen (smaller y).
     */
    public Point2D.Double project(double tx, double ty, double elevation) {
        double dx = tx - camTileX;
        double dy = ty - camTileY;
        double sx = viewW / 2.0 + (dx - dy) * TILE_HW;
        double sy = viewH / 2.0 + (dx + dy) * TILE_HH - elevation * ELEV_LIFT;
        return new Point2D.Double(sx, sy);
    }

    /** Rough inverse projection at sea level (elevation ignored) for mouse picks. */
    public Point2D.Double unproject(int screenX, int screenY) {
        double a = (screenX - viewW / 2.0) / TILE_HW;   // dx - dy
        double b = (screenY - viewH / 2.0) / TILE_HH;   // dx + dy
        double dx = (a + b) / 2.0;
        double dy = (b - a) / 2.0;
        return new Point2D.Double(camTileX + dx, camTileY + dy);
    }

    /** Draw a single terrain tile (diamond top + earth wall + drop shadow). */
    public void drawTile(Graphics2D g, GameStateView st, TileView t) {
        int elev = t.elevation();
        TerrainType terrain = t.terrain();
        Point2D.Double top = project(t.x(), t.y(), elev);

        // Diamond top corners (N, E, S, W) around the projected centre.
        int cx = (int) Math.round(top.x);
        int cy = (int) Math.round(top.y);
        Polygon dia = diamond(cx, cy);

        // Drop-shadow: offset diamond at sea level, translucent.
        Point2D.Double base = project(t.x(), t.y(), 0);
        if (elev > 0) {
            Polygon sh = diamond((int) Math.round(base.x) + 4,
                                 (int) Math.round(base.y) + 3);
            g.setColor(UiPalette.SHADOW);
            g.fillPolygon(sh);
        }

        boolean water = terrain == TerrainType.DEEP_WATER
                     || terrain == TerrainType.SHALLOW_WATER;

        // Earth wall dropping from the diamond down to sea level (only if lifted).
        if (elev > 0 && !water) {
            Color side = UiPalette.darken(baseColor(t), 0.42);
            // Left face
            Polygon left = new Polygon();
            left.addPoint(cx - TILE_HW, cy);
            left.addPoint(cx,           cy + TILE_HH);
            left.addPoint(cx,           cy + TILE_HH + elev * ELEV_LIFT);
            left.addPoint(cx - TILE_HW, cy + elev * ELEV_LIFT);
            g.setColor(side);
            g.fillPolygon(left);
            // Right face (a touch brighter for a lit edge)
            Polygon right = new Polygon();
            right.addPoint(cx + TILE_HW, cy);
            right.addPoint(cx,           cy + TILE_HH);
            right.addPoint(cx,           cy + TILE_HH + elev * ELEV_LIFT);
            right.addPoint(cx + TILE_HW, cy + elev * ELEV_LIFT);
            g.setColor(UiPalette.darken(baseColor(t), 0.28));
            g.fillPolygon(right);
        }

        // Diamond top, shaded by elevation (higher = lighter).
        Color face = baseColor(t);
        double lift = Math.min(0.35, elev * 0.05);
        face = UiPalette.lighten(face, lift);
        if (t.snowCovered()) {
            face = UiPalette.lighten(UiPalette.SNOW, Math.min(0.2, elev * 0.03));
        }
        g.setColor(face);
        g.fillPolygon(dia);

        // Subtle water shimmer bands.
        if (water) {
            g.setColor(UiPalette.lighten(face, 0.18));
            g.drawLine(cx - TILE_HW / 2, cy + TILE_HH / 2, cx, cy);
            g.drawLine(cx + TILE_HW / 2, cy - TILE_HH / 2, cx, cy);
        }

        // Grid edge.
        g.setColor(UiPalette.darken(face, 0.30));
        g.drawPolygon(dia);
    }

    private Color baseColor(TileView t) {
        TerrainType terrain = t.terrain();
        if (terrain == TerrainType.FOREST) {
            return UiPalette.FOREST;
        }
        return UiPalette.terrain(terrain);
    }

    private Polygon diamond(int cx, int cy) {
        Polygon p = new Polygon();
        p.addPoint(cx,           cy - TILE_HH); // N
        p.addPoint(cx + TILE_HW, cy);           // E
        p.addPoint(cx,           cy + TILE_HH); // S
        p.addPoint(cx - TILE_HW, cy);           // W
        return p;
    }
}
