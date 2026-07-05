package com.whim.populous.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;

import com.whim.populous.api.Enums.Allegiance;
import com.whim.populous.api.Enums.SettlementType;
import com.whim.populous.api.Enums.TerrainType;
import com.whim.populous.api.Views.FollowerView;
import com.whim.populous.api.Views.MapView;
import com.whim.populous.api.Views.PapalMagnetView;
import com.whim.populous.api.Views.TileView;

/**
 * Pure {@link Graphics2D} drawing helpers. Strict top-down view (chosen for
 * reliability): elevation is conveyed by {@link UiColors}'s colour ramp plus a
 * subtle top-left highlight / bottom-right shadow on each tile, with contour
 * lines at band boundaries. Stateless — all context passed in per call.
 */
public final class Renderer {

    private Renderer() { }

    public static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    /**
     * Paint one terrain tile at pixel (px,py) of size {@code ts}. The elevation
     * relative to sea level drives both the colour ramp and a fake-3D bevel so
     * the flat map still reads as a heightfield.
     */
    public static void drawTile(Graphics2D g, TileView t, int px, int py, int ts, int seaLevel) {
        TerrainType terr = t.terrain();
        int rel = t.elevation() - seaLevel;
        double norm = clamp(rel / 6.0, -1.0, 1.0);
        Color base = UiColors.terrainShaded(terr, norm);
        g.setColor(base);
        g.fillRect(px, py, ts, ts);

        // Subtle bevel: brighter top/left edge, darker bottom/right => relief.
        if (terr != TerrainType.WATER && terr != TerrainType.SHALLOW) {
            g.setColor(UiColors.withAlpha(Color.WHITE, 26));
            g.drawLine(px, py, px + ts - 1, py);
            g.drawLine(px, py, px, py + ts - 1);
            g.setColor(UiColors.withAlpha(Color.BLACK, 34));
            g.drawLine(px, py + ts - 1, px + ts - 1, py + ts - 1);
            g.drawLine(px + ts - 1, py, px + ts - 1, py + ts - 1);
        }
    }

    /**
     * Contour tint drawn on top of a tile when its neighbour to the right/below
     * is a different elevation band — a thin darker seam that makes plateaus and
     * cliffs pop. Cheap; called during the terrain pass.
     */
    public static void drawContour(Graphics2D g, MapView map, int col, int row,
                                   int px, int py, int ts) {
        int e = map.tileAt(col, row).elevation();
        g.setColor(UiColors.withAlpha(Color.BLACK, 46));
        if (col + 1 < map.cols() && map.tileAt(col + 1, row).elevation() != e) {
            g.drawLine(px + ts - 1, py, px + ts - 1, py + ts - 1);
        }
        if (row + 1 < map.rows() && map.tileAt(col, row + 1).elevation() != e) {
            g.drawLine(px, py + ts - 1, px + ts - 1, py + ts - 1);
        }
    }

    /**
     * A settlement drawn as an escalating shape by tier:
     * TENT (triangle) -> HUT (peaked box) -> HOUSE (box + roof) ->
     * TOWER (tall keep) -> CASTLE (crenellated block). Coloured by owner.
     */
    public static void drawSettlement(Graphics2D g, SettlementType type, Allegiance owner,
                                      int px, int py, int ts) {
        if (type == null || type == SettlementType.NONE) {
            return;
        }
        Color body = UiColors.allegianceColor(owner);
        Color dark = UiColors.allegianceDark(owner);
        int cx = px + ts / 2;
        int by = py + ts - 2;                  // baseline
        int w = ts;

        switch (type) {
            case TENT: {
                Polygon p = tri(cx, py + ts / 4, w * 5 / 12, by);
                fillOutline(g, p, body, dark);
                break;
            }
            case HUT: {
                int hw = w * 5 / 12;
                int roof = py + ts * 2 / 5;
                g.setColor(body);
                g.fillRect(cx - hw, roof, hw * 2, by - roof);
                fillOutline(g, tri(cx, py + ts / 5, hw + 1, roof), dark, dark);
                g.setColor(dark);
                g.drawRect(cx - hw, roof, hw * 2, by - roof);
                break;
            }
            case HOUSE: {
                int hw = w * 5 / 12;
                int wallTop = py + ts / 2;
                g.setColor(body);
                g.fillRect(cx - hw, wallTop, hw * 2, by - wallTop);
                fillOutline(g, tri(cx, py + ts / 6, hw + 2, wallTop), dark, dark);
                g.setColor(UiColors.lighten(body, 0.4));
                g.fillRect(cx - 2, wallTop + 2, 4, 5); // door
                g.setColor(dark);
                g.drawRect(cx - hw, wallTop, hw * 2, by - wallTop);
                break;
            }
            case TOWER: {
                int hw = w * 3 / 12;
                int top = py + ts / 6;
                g.setColor(body);
                g.fillRect(cx - hw, top, hw * 2, by - top);
                g.setColor(dark);
                // battlement teeth
                for (int i = 0; i < 3; i++) {
                    g.fillRect(cx - hw + i * hw, top - 3, hw - 1, 4);
                }
                g.drawRect(cx - hw, top, hw * 2, by - top);
                g.setColor(UiColors.lighten(body, 0.35));
                g.fillRect(cx - 2, py + ts / 3, 4, 5); // window
                break;
            }
            case CASTLE: {
                int hw = w * 5 / 12;
                int top = py + ts / 5;
                g.setColor(body);
                g.fillRect(cx - hw, top, hw * 2, by - top);
                g.setColor(dark);
                int teeth = 4;
                int tw = (hw * 2) / teeth;
                for (int i = 0; i < teeth; i++) {
                    g.fillRect(cx - hw + i * tw, top - 4, tw - 1, 5);
                }
                // corner turrets
                g.fillRect(cx - hw - 2, top, 4, by - top);
                g.fillRect(cx + hw - 2, top, 4, by - top);
                g.drawRect(cx - hw, top, hw * 2, by - top);
                g.setColor(UiColors.lighten(body, 0.4));
                g.fillRect(cx - 3, by - 8, 6, 8); // gate
                break;
            }
            default:
                break;
        }
    }

    /** A follower: a small square dot coloured by allegiance, dimmed if weak. */
    public static void drawFollower(Graphics2D g, FollowerView f, int px, int py, int size) {
        Color c = UiColors.allegianceColor(f.allegiance());
        if (f.health() < 40) {
            c = UiColors.blend(c, Color.DARK_GRAY, 0.45);
        }
        g.setColor(UiColors.withAlpha(Color.BLACK, 90));
        g.fillOval(px - size / 2 + 1, py - size / 2 + 1, size, size);
        g.setColor(c);
        g.fillRect(px - size / 2, py - size / 2, size, size);
        g.setColor(UiColors.allegianceDark(f.allegiance()));
        g.drawRect(px - size / 2, py - size / 2, size, size);
    }

    /** Papal magnet marker: a pulsing ringed cross at the rally tile. */
    public static void drawMagnet(Graphics2D g, PapalMagnetView m, int px, int py, int ts) {
        if (m == null || !m.active()) {
            return;
        }
        Color c = UiColors.allegianceColor(m.side());
        int cx = px + ts / 2;
        int cy = py + ts / 2;
        int r = ts;
        g.setStroke(new BasicStroke(2f));
        g.setColor(UiColors.withAlpha(c, 200));
        g.drawOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(UiColors.withAlpha(c, 120));
        g.drawOval(cx - r / 2, cy - r / 2, r, r);
        // staff + banner
        g.setColor(c);
        g.fillRect(cx - 1, cy - r, 2, r);
        Polygon flag = new Polygon();
        flag.addPoint(cx + 1, cy - r);
        flag.addPoint(cx + 1 + ts / 2, cy - r + ts / 6);
        flag.addPoint(cx + 1, cy - r + ts / 3);
        g.fill(flag);
        g.setStroke(new BasicStroke(1f));
    }

    // ---- helpers -------------------------------------------------------------

    private static Polygon tri(int apexX, int apexY, int halfW, int baseY) {
        Polygon p = new Polygon();
        p.addPoint(apexX, apexY);
        p.addPoint(apexX - halfW, baseY);
        p.addPoint(apexX + halfW, baseY);
        return p;
    }

    private static void fillOutline(Graphics2D g, Polygon p, Color fill, Color line) {
        g.setColor(fill);
        g.fillPolygon(p);
        g.setColor(line);
        g.drawPolygon(p);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
