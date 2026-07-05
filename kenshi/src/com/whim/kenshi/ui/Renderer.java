package com.whim.kenshi.ui;

import com.whim.kenshi.api.Config;
import com.whim.kenshi.api.Enums;
import com.whim.kenshi.api.Views;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * All world drawing, using {@link Graphics2D} primitives only. Nothing here
 * mutates state; every method reads {@link Views} snapshots and a {@link Camera}
 * and paints. Terrain is tinted tiles, towns/nodes are labelled patches and
 * characters are faction-coloured circles with a heading wedge, selection ring,
 * floating health pip and distinct DOWNED / DEAD / CRAWLING visuals.
 */
public final class Renderer {

    private final Font labelFont = new Font("SansSerif", Font.BOLD, 12);
    private final Font tinyFont  = new Font("SansSerif", Font.PLAIN, 10);

    // ---- terrain ---------------------------------------------------------
    public void drawTerrain(Graphics2D g, Views.MapView map, Camera cam) {
        int tiles = map.tiles();
        double ts = map.tileSize();

        // Only iterate the tiles that intersect the viewport.
        int c0 = (int) Math.floor(cam.toWorldX(0) / ts) - 1;
        int r0 = (int) Math.floor(cam.toWorldY(0) / ts) - 1;
        int c1 = (int) Math.ceil(cam.toWorldX(cam.viewW()) / ts) + 1;
        int r1 = (int) Math.ceil(cam.toWorldY(cam.viewH()) / ts) + 1;
        if (c0 < 0) c0 = 0;
        if (r0 < 0) r0 = 0;
        if (c1 > tiles) c1 = tiles;
        if (r1 > tiles) r1 = tiles;

        double tsScreen = cam.scale(ts);
        // +2 pixels of overlap to avoid seams from double rounding.
        int w = (int) Math.ceil(tsScreen) + 1;
        int h = w;

        for (int row = r0; row < r1; row++) {
            for (int col = c0; col < c1; col++) {
                Enums.Terrain t = map.terrain(col, row);
                g.setColor(Palette.terrain(t));
                int sx = (int) Math.floor(cam.toScreenX(col * ts));
                int sy = (int) Math.floor(cam.toScreenY(row * ts));
                g.fillRect(sx, sy, w, h);
            }
        }
    }

    // ---- world nodes (towns, bars, camps, ruins) -------------------------
    public void drawNode(Graphics2D g, Views.NodeView n, Camera cam) {
        double sx = cam.toScreenX(n.x());
        double sy = cam.toScreenY(n.y());
        double r = Math.max(6.0, cam.scale(n.radius()));

        Color fill = Palette.faction(n.owner());
        g.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 46));
        g.fill(new Ellipse2D.Double(sx - r, sy - r, r * 2, r * 2));
        g.setStroke(new BasicStroke(1.6f));
        g.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 190));
        g.draw(new Ellipse2D.Double(sx - r, sy - r, r * 2, r * 2));

        // A small icon marks the node type at its centre.
        drawNodeIcon(g, n.type(), sx, sy);

        // Label towns / named locations above the patch.
        g.setFont(labelFont);
        String label = n.name();
        int tw = g.getFontMetrics().stringWidth(label);
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(label, (int) (sx - tw / 2) + 1, (int) (sy - r - 5) + 1);
        g.setColor(Palette.HUD_TEXT);
        g.drawString(label, (int) (sx - tw / 2), (int) (sy - r - 5));
    }

    private void drawNodeIcon(Graphics2D g, Enums.NodeType type, double sx, double sy) {
        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(20, 18, 16, 210));
        switch (type) {
            case TOWN:
                g.fill(new Rectangle2D.Double(sx - 5, sy - 5, 10, 10));
                break;
            case BAR:
                g.fill(new Ellipse2D.Double(sx - 4, sy - 4, 8, 8));
                break;
            case SHOP:
                g.fill(new Rectangle2D.Double(sx - 5, sy - 3, 10, 6));
                break;
            case CAMP: {
                Path2D.Double tri = new Path2D.Double();
                tri.moveTo(sx, sy - 6);
                tri.lineTo(sx - 6, sy + 5);
                tri.lineTo(sx + 6, sy + 5);
                tri.closePath();
                g.fill(tri);
                break;
            }
            case RUIN:
                g.draw(new Line2D.Double(sx - 5, sy + 5, sx - 5, sy - 4));
                g.draw(new Line2D.Double(sx, sy + 5, sx, sy - 6));
                g.draw(new Line2D.Double(sx + 5, sy + 5, sx + 5, sy - 3));
                break;
            default:
                break;
        }
    }

    // ---- characters ------------------------------------------------------
    public void drawCharacter(Graphics2D g, Views.CharacterView ch, Camera cam) {
        double sx = cam.toScreenX(ch.x());
        double sy = cam.toScreenY(ch.y());
        double r = Math.max(4.0, cam.scale(Config.CHAR_RADIUS));

        Enums.MoveState ms = ch.moveState();
        boolean dead = ms == Enums.MoveState.DEAD;
        boolean downed = ms == Enums.MoveState.DOWNED;
        boolean crawling = ms == Enums.MoveState.CRAWLING;

        Color base = Palette.faction(ch.faction());
        if (dead || downed) {
            base = desaturate(base, dead ? 0.75 : 0.5);
        }

        // Selection ring underneath.
        if (ch.selected()) {
            g.setStroke(new BasicStroke(2.4f));
            g.setColor(Palette.SELECT_RING);
            g.draw(new Ellipse2D.Double(sx - r - 4, sy - r - 4, (r + 4) * 2, (r + 4) * 2));
        }

        // Shadow.
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Ellipse2D.Double(sx - r * 0.9, sy - r * 0.5 + r, r * 1.8, r * 0.7));

        // Body.
        g.setColor(base);
        g.fill(new Ellipse2D.Double(sx - r, sy - r, r * 2, r * 2));
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(Palette.darker(base, 0.55));
        g.draw(new Ellipse2D.Double(sx - r, sy - r, r * 2, r * 2));

        // Heading wedge (skip for dead/downed — they are prone).
        if (!dead && !downed) {
            drawHeadingWedge(g, sx, sy, r, ch.heading(), base);
        }

        // State overlays.
        if (dead) {
            drawDeadCross(g, sx, sy, r);
        } else if (downed) {
            drawProneMarker(g, sx, sy, r, new Color(60, 60, 66));
        } else if (crawling) {
            drawProneMarker(g, sx, sy, r, new Color(210, 170, 70));
        }

        // Floating health pip (worst-part fraction) above able-bodied units.
        if (!dead) {
            drawHealthPip(g, ch, sx, sy - r - 6, r);
        }

        // Bleed droplet indicator.
        if (ch.bleedRate() > 0.01 && !dead) {
            g.setColor(Palette.BLEED);
            double bx = sx + r * 0.7;
            double by = sy - r * 0.7;
            g.fill(new Ellipse2D.Double(bx - 2, by - 2, 4, 5));
        }
    }

    private void drawHeadingWedge(Graphics2D g, double sx, double sy, double r,
                                  double heading, Color base) {
        double len = r * 1.7;
        double half = 0.42; // radians
        Path2D.Double wedge = new Path2D.Double();
        wedge.moveTo(sx, sy);
        wedge.lineTo(sx + Math.cos(heading - half) * len, sy + Math.sin(heading - half) * len);
        wedge.lineTo(sx + Math.cos(heading) * (len * 1.12), sy + Math.sin(heading) * (len * 1.12));
        wedge.lineTo(sx + Math.cos(heading + half) * len, sy + Math.sin(heading + half) * len);
        wedge.closePath();
        g.setColor(Palette.lerp(base, Color.WHITE, 0.5));
        g.fill(wedge);
    }

    private void drawDeadCross(Graphics2D g, double sx, double sy, double r) {
        g.setStroke(new BasicStroke(2.4f));
        g.setColor(new Color(180, 40, 36));
        double d = r * 0.8;
        g.draw(new Line2D.Double(sx - d, sy - d, sx + d, sy + d));
        g.draw(new Line2D.Double(sx - d, sy + d, sx + d, sy - d));
    }

    private void drawProneMarker(Graphics2D g, double sx, double sy, double r, Color c) {
        // A horizontal bar suggesting the character is lying down.
        g.setStroke(new BasicStroke(2.2f));
        g.setColor(c);
        g.draw(new Line2D.Double(sx - r * 0.7, sy, sx + r * 0.7, sy));
    }

    private void drawHealthPip(Graphics2D g, Views.CharacterView ch,
                               double sx, double topY, double r) {
        double frac = worstPartFraction(ch);
        double w = Math.max(14.0, r * 1.8);
        double h = 3.0;
        double x = sx - w / 2;
        g.setColor(new Color(0, 0, 0, 150));
        g.fill(new Rectangle2D.Double(x - 1, topY - 1, w + 2, h + 2));
        g.setColor(new Color(40, 38, 36));
        g.fill(new Rectangle2D.Double(x, topY, w, h));
        g.setColor(Palette.grade(frac));
        g.fill(new Rectangle2D.Double(x, topY, w * Math.max(0.0, frac), h));
    }

    /** Fraction (0..1) of the character's worst body part, for the floating pip. */
    static double worstPartFraction(Views.CharacterView ch) {
        double worst = 1.0;
        Enums.BodyPart[] parts = Enums.BodyPart.values();
        for (int i = 0; i < parts.length; i++) {
            double max = ch.partMax(parts[i]);
            if (max <= 0) continue;
            double f = ch.partHp(parts[i]) / max;
            if (f < worst) worst = f;
        }
        if (worst < 0) worst = 0;
        return worst;
    }

    private static Color desaturate(Color c, double amt) {
        int grey = (int) (c.getRed() * 0.3 + c.getGreen() * 0.59 + c.getBlue() * 0.11);
        return Palette.lerp(c, new Color(grey, grey, grey), amt);
    }

    Font tinyFont() { return tinyFont; }
}
