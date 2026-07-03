package com.whim.powermonger.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;

import com.whim.powermonger.api.Enums.Allegiance;
import com.whim.powermonger.api.Enums.Job;
import com.whim.powermonger.api.Enums.Posture;
import com.whim.powermonger.api.Views.CaptainView;
import com.whim.powermonger.api.Views.PigeonView;
import com.whim.powermonger.api.Views.TownspersonView;

/**
 * Procedural geometric sprites drawn with {@code Graphics2D}: captains/blocs
 * (banner pole, faction pennant, strength pips, posture swords), townspeople by
 * {@link Job}, trees, and flocks of birds as small moving chevrons. Each ground
 * unit gets a soft drop-shadow so it reads against the terrain.
 */
public final class SpriteFactory {
    private SpriteFactory() {}

    /** Elliptical ground shadow centred at (sx, sy). */
    public static void shadow(Graphics2D g, int sx, int sy, int rw, int rh) {
        g.setColor(UiPalette.SHADOW);
        g.fill(new Ellipse2D.Double(sx - rw, sy - rh / 2.0, rw * 2, rh));
    }

    /** A tree: dark trunk + layered conifer canopy, with drop-shadow. */
    public static void tree(Graphics2D g, int sx, int sy) {
        shadow(g, sx, sy, 8, 6);
        g.setColor(new Color(78, 54, 32));
        g.fillRect(sx - 2, sy - 8, 4, 9);
        for (int i = 0; i < 3; i++) {
            int w = 16 - i * 3;
            int yy = sy - 8 - i * 8;
            Polygon p = new Polygon();
            p.addPoint(sx, yy - 12);
            p.addPoint(sx + w, yy);
            p.addPoint(sx - w, yy);
            g.setColor(i == 2 ? UiPalette.lighten(UiPalette.FOREST, 0.18)
                              : UiPalette.darken(UiPalette.FOREST, i * 0.08));
            g.fillPolygon(p);
        }
    }

    /** A townsperson: small torso dot tinted by job, with a job glyph. */
    public static void townsperson(Graphics2D g, int sx, int sy, TownspersonView p) {
        shadow(g, sx, sy, 6, 4);
        Color body = UiPalette.job(p.job());
        g.setColor(UiPalette.darken(body, 0.2));
        g.fillOval(sx - 4, sy - 12, 8, 8);          // head/torso
        g.setColor(body);
        g.fillRect(sx - 3, sy - 6, 6, 6);           // smock
        // tiny job tool glyph
        g.setColor(UiPalette.INK);
        Job j = p.job();
        if (j == Job.FARMING) {
            g.drawLine(sx + 4, sy - 10, sx + 4, sy - 2);
        } else if (j == Job.FISHING) {
            g.drawLine(sx + 4, sy - 10, sx + 7, sy - 4);
        } else if (j == Job.CRAFTING) {
            g.fillRect(sx + 3, sy - 8, 3, 3);
        } else if (j == Job.HERDING) {
            g.drawOval(sx + 3, sy - 6, 4, 4);
        }
    }

    /**
     * A captain's army bloc: banner pole, faction pennant, a ring of strength
     * pips and the posture swords. Selected blocs get a highlight ring.
     */
    public static void captain(Graphics2D g, int sx, int sy, CaptainView c) {
        Color fac = UiPalette.faction(c.allegiance());
        shadow(g, sx, sy, 12, 7);

        // Selection / commander ring.
        if (c.selected()) {
            g.setColor(UiPalette.HILIGHT);
            g.setStroke(new BasicStroke(2f));
            g.drawOval(sx - 13, sy - 8, 26, 16);
        }
        if (c.supremeCommander()) {
            g.setColor(UiPalette.lighten(fac, 0.4));
            g.drawOval(sx - 15, sy - 9, 30, 18);
        }

        // Base platform.
        g.setColor(UiPalette.darken(fac, 0.35));
        g.fillOval(sx - 10, sy - 6, 20, 12);
        g.setColor(fac);
        g.fillOval(sx - 8, sy - 5, 16, 10);

        // Banner pole + pennant.
        int poleTop = sy - 34;
        g.setColor(new Color(60, 44, 28));
        g.setStroke(new BasicStroke(2f));
        g.drawLine(sx, sy - 6, sx, poleTop);
        Polygon pennant = new Polygon();
        pennant.addPoint(sx, poleTop);
        pennant.addPoint(sx + 18, poleTop + 5);
        pennant.addPoint(sx, poleTop + 11);
        g.setColor(fac);
        g.fillPolygon(pennant);
        g.setColor(UiPalette.darken(fac, 0.4));
        g.drawPolygon(pennant);

        // Strength pips arced above the base (1..5 by strength bracket).
        int pips = strengthPips(c.strength());
        g.setColor(UiPalette.HILIGHT);
        for (int i = 0; i < pips; i++) {
            int px = sx - 8 + i * 4;
            g.fillOval(px, sy - 12, 3, 3);
        }

        // Posture swords next to the base.
        drawSwords(g, sx + 10, sy - 2, c.posture(), fac);
    }

    private static void drawSwords(Graphics2D g, int sx, int sy, Posture posture, Color fac) {
        int n = posture == null ? 1 : posture.swords();
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(1.6f));
        for (int i = 0; i < n; i++) {
            int x = sx + i * 4;
            g.setColor(new Color(210, 210, 220));
            g.drawLine(x, sy, x, sy - 10);      // blade
            g.setColor(new Color(120, 96, 40));
            g.drawLine(x - 2, sy - 8, x + 2, sy - 8); // guard
        }
        g.setStroke(old);
    }

    private static int strengthPips(int strength) {
        if (strength <= 0) return 0;
        if (strength < 20) return 1;
        if (strength < 50) return 2;
        if (strength < 90) return 3;
        if (strength < 140) return 4;
        return 5;
    }

    /**
     * A flock of birds as small moving chevrons. {@code phase} animates the
     * wing-flap; birds scatter around (sx, sy).
     */
    public static void flock(Graphics2D g, int sx, int sy, int count, double phase) {
        g.setColor(new Color(30, 26, 22));
        g.setStroke(new BasicStroke(1.4f));
        for (int i = 0; i < count; i++) {
            double a = i * 2.399963; // golden-angle scatter
            int bx = sx + (int) Math.round(Math.cos(a) * (6 + i * 3));
            int by = sy + (int) Math.round(Math.sin(a) * (4 + i * 2));
            int flap = (int) Math.round(3 + 2 * Math.sin(phase + i));
            g.drawLine(bx - 4, by, bx, by - flap);
            g.drawLine(bx, by - flap, bx + 4, by);
        }
    }

    /** A carrier pigeon in flight: a single white chevron with a faint trail. */
    public static void pigeon(Graphics2D g, int sx, int sy, PigeonView p, double phase) {
        int flap = (int) Math.round(3 + 2 * Math.sin(phase * 2));
        g.setColor(new Color(235, 235, 240));
        g.setStroke(new BasicStroke(2f));
        g.drawLine(sx - 5, sy, sx, sy - flap);
        g.drawLine(sx, sy - flap, sx + 5, sy);
        g.setColor(new Color(235, 235, 240, 70));
        g.fillOval(sx - 2, sy - 2, 4, 4);
    }

    static Color factionOf(Allegiance a) { return UiPalette.faction(a); }
}
