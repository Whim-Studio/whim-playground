package com.whim.albion.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import com.whim.albion.api.GameController;
import com.whim.albion.api.Views.PlayerView;
import com.whim.albion.api.Views.TileView;
import com.whim.albion.api.Views.WorldView;
import com.whim.albion.api.Enums.Direction;
import com.whim.albion.api.Enums.TileType;

/**
 * Grid-based pseudo-3D "blobber" renderer for INDOOR_3D maps.
 *
 * <p>Projection approach (documented in task3-notes.md): we march forward from the player
 * along the facing direction for a fixed number of depth steps. At each depth {@code d} we
 * define a screen rectangle (a "corridor slice") whose size shrinks toward a central
 * vanishing point via a perspective scale {@code s(d)}. For each slice we:
 * <ol>
 *   <li>draw the left and right side walls as trapezoids if the neighbouring cell at that
 *       depth blocks sight, shaded darker with depth;</li>
 *   <li>if the cell straight ahead blocks sight, draw a front wall quad closing the corridor
 *       and stop marching;</li>
 *   <li>otherwise draw the floor/ceiling bands for that slice and continue.</li>
 * </ol>
 * Everything is flat-shaded polygons — no textures, no raycasting, purely grid-cell based.
 */
final class FirstPersonRenderer extends JPanel {

    private static final int MAX_DEPTH = 5;

    private final GameController controller;

    FirstPersonRenderer(GameController controller) {
        this.controller = controller;
        setBackground(Color.BLACK);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        WorldView w = controller.state().world();
        if (w == null) return;
        int width = getWidth(), height = getHeight();
        int cx = width / 2, cy = height / 2;

        // ceiling + floor background
        g.setColor(new Color(30, 28, 34));
        g.fillRect(0, 0, width, cy);
        g.setColor(new Color(44, 38, 32));
        g.fillRect(0, cy, width, height - cy);

        PlayerView p = w.player();
        Direction fwd = p.facing();
        Direction left = fwd.left();
        Direction right = fwd.right();

        // perspective half-widths at each depth boundary (fraction of screen half-width)
        float[] scale = new float[MAX_DEPTH + 1];
        for (int d = 0; d <= MAX_DEPTH; d++) {
            scale[d] = 1f / (1f + d * 0.85f);
        }
        int halfW = width / 2;
        int halfH = (int) (height * 0.44);

        for (int d = 0; d < MAX_DEPTH; d++) {
            int cxFwd = p.x() + fwd.dx() * d;
            int cyFwd = p.y() + fwd.dy() * d;

            float sNear = scale[d];
            float sFar = scale[d + 1];
            int nearHalfW = (int) (halfW * sNear);
            int farHalfW = (int) (halfW * sFar);
            int nearHalfH = (int) (halfH * sNear);
            int farHalfH = (int) (halfH * sFar);

            int nearL = cx - nearHalfW, nearR = cx + nearHalfW;
            int farL = cx - farHalfW, farR = cx + farHalfW;
            int nearT = cy - nearHalfH, nearB = cy + nearHalfH;
            int farT = cy - farHalfH, farB = cy + farHalfH;

            // floor slice
            g.setColor(shade(new Color(78, 64, 50), d));
            fillQuad(g, nearL, nearB, nearR, nearB, farR, farB, farL, farB);
            // ceiling slice
            g.setColor(shade(new Color(52, 50, 62), d));
            fillQuad(g, nearL, nearT, nearR, nearT, farR, farT, farL, farT);

            // side walls: if the cell to the left/right at this depth blocks sight
            TileView leftTile = w.tileAt(cxFwd + left.dx(), cyFwd + left.dy());
            if (blocks(leftTile)) {
                g.setColor(shade(wallColor(leftTile.type()).darker(), d));
                fillQuad(g, nearL, nearT, farL, farT, farL, farB, nearL, nearB);
            }
            TileView rightTile = w.tileAt(cxFwd + right.dx(), cyFwd + right.dy());
            if (blocks(rightTile)) {
                g.setColor(shade(wallColor(rightTile.type()).darker(), d));
                fillQuad(g, nearR, nearT, farR, farT, farR, farB, nearR, nearB);
            }

            // front wall: the cell straight ahead at depth d+1
            TileView aheadTile = w.tileAt(p.x() + fwd.dx() * (d + 1), p.y() + fwd.dy() * (d + 1));
            if (blocks(aheadTile)) {
                g.setColor(shade(wallColor(aheadTile.type()), d + 1));
                g.fillRect(farL, farT, farR - farL, farB - farT);
                g.setColor(new Color(0, 0, 0, 90));
                g.drawRect(farL, farT, farR - farL, farB - farT);
                // draw a door hint
                if (aheadTile.type() == TileType.DOOR) {
                    g.setColor(new Color(150, 110, 60));
                    int dw = (farR - farL) / 3, dh = (farB - farT) * 3 / 4;
                    g.fillRect(cx - dw / 2, farB - dh, dw, dh);
                }
                break; // corridor is closed; nothing further is visible
            }

            // stairs marker on the floor ahead
            if (aheadTile.type() == TileType.STAIRS) {
                g.setColor(shade(new Color(120, 120, 150), d));
                g.fillRect(cx - farHalfW / 2, farB - 6, farHalfW, 6);
            }
        }

        // compass + facing HUD
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, width, 22);
        g.setColor(new Color(210, 200, 170));
        g.drawString(w.mapName() + "   facing " + fwd + "   (↑/↓ step · ←/→ turn · E to interact)", 8, 16);
    }

    private static boolean blocks(TileView t) {
        return t != null && (t.blocksSight() || t.type() == TileType.WALL
                || t.type() == TileType.DOOR || t.type() == TileType.OBSTACLE);
    }

    private static Color wallColor(TileType t) {
        switch (t) {
            case DOOR:     return new Color(96, 68, 40);
            case OBSTACLE: return new Color(60, 60, 66);
            default:       return new Color(96, 90, 84);
        }
    }

    private static Color shade(Color c, int depth) {
        float f = Math.max(0.35f, 1f - depth * 0.16f);
        return new Color((int) (c.getRed() * f), (int) (c.getGreen() * f), (int) (c.getBlue() * f));
    }

    private static void fillQuad(Graphics2D g, int... xy) {
        Polygon p = new Polygon();
        for (int i = 0; i < xy.length; i += 2) p.addPoint(xy[i], xy[i + 1]);
        g.fillPolygon(p);
    }

    @Override public Dimension getPreferredSize() { return new Dimension(640, 520); }
}
