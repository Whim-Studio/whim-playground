package com.whim.samurai.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;

/**
 * Procedural Java2D figures for the action sequences — no ripped art (design ref
 * §0, constraints). All shapes are drawn from primitives so the game ships with
 * only placeholder-quality but original artwork.
 *
 * <ul>
 *   <li>{@link #drawDuelist} — a side-view katana fighter with distinct stance /
 *       swing poses for the DUEL engine (design ref §2a).</li>
 *   <li>{@link #drawHero} / {@link #drawGuard} — top-down glyphs for the MELEE /
 *       ninja engine (design ref §2c).</li>
 *   <li>{@link #drawSoldier} / {@link #drawBanner} — army glyphs for the BATTLE
 *       engine (design ref §2b).</li>
 * </ul>
 */
public final class SamuraiSprite {
    private SamuraiSprite() { }

    // --- Duel poses (side view). Numbers are pose codes used by DuelScreen. ---
    public static final int POSE_GUARD_MID = 0;
    public static final int POSE_GUARD_HIGH = 1;
    public static final int POSE_GUARD_LOW = 2;
    public static final int POSE_WINDUP = 3;   // charged over-the-shoulder wind-up (§2a)
    public static final int POSE_STRIKE_HIGH = 4;
    public static final int POSE_STRIKE_MID = 5;
    public static final int POSE_STRIKE_LOW = 6;
    public static final int POSE_PARRY = 7;
    public static final int POSE_STAGGER = 8;  // knocked back by a wound (§2a)

    /**
     * Draw a side-view duelist standing with feet at (footX, footY), scaled by
     * {@code s} (~1.0 baseline), facing {@code +1} (right) or {@code -1} (left).
     */
    public static void drawDuelist(Graphics2D g, int footX, int footY, double s,
                                   int facing, Color kimono, Color trim, int pose) {
        Stroke old = g.getStroke();
        // ground shadow
        g.setColor(new Color(0, 0, 0, 40));
        g.fill(new Ellipse2D.Double(footX - 26 * s, footY - 6 * s, 52 * s, 12 * s));

        double hipY = footY - 44 * s;
        double shoulderY = footY - 78 * s;
        double headY = footY - 92 * s;
        double lean = pose == POSE_STAGGER ? -facing * 10 * s : 0;

        // legs (a braced kenjutsu stance)
        g.setColor(Palette.INK);
        g.setStroke(new BasicStroke((float) (6 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine((int) footX, (int) hipY, (int) (footX - 16 * s), (int) footY);
        g.drawLine((int) footX, (int) hipY, (int) (footX + 16 * s), (int) footY);

        // torso (kimono)
        g.setColor(kimono);
        g.setStroke(new BasicStroke((float) (18 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine((int) (footX + lean), (int) hipY, (int) (footX + lean * 1.5), (int) shoulderY);
        // sash / obi
        g.setColor(trim);
        g.setStroke(new BasicStroke((float) (5 * s)));
        g.drawLine((int) (footX - 8 * s + lean), (int) (hipY - 4 * s),
                   (int) (footX + 8 * s + lean), (int) (hipY - 4 * s));

        // head + topknot
        double headX = footX + lean * 1.6 + facing * 2 * s;
        g.setColor(new Color(230, 205, 170));
        g.fillOval((int) (headX - 8 * s), (int) (headY - 8 * s), (int) (16 * s), (int) (16 * s));
        g.setColor(Palette.INK);
        g.fillOval((int) (headX - 3 * s), (int) (headY - 14 * s), (int) (6 * s), (int) (8 * s)); // chonmage

        // sword — endpoints depend on pose
        double gx = footX + lean * 1.5, gy = shoulderY + 6 * s; // grip near shoulder
        double[] tip = swordTip(pose, facing, s, gx, gy);
        double hx = tip[2], hy = tip[3]; // hilt (hand) position
        // arm to hilt
        g.setColor(kimono.darker());
        g.setStroke(new BasicStroke((float) (7 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine((int) (footX + lean * 1.4), (int) (shoulderY + 4 * s), (int) hx, (int) hy);
        // blade
        boolean charged = pose == POSE_WINDUP;
        g.setColor(charged ? Palette.CINNABAR : new Color(210, 216, 224));
        g.setStroke(new BasicStroke((float) ((charged ? 4.5 : 3.5) * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine((int) hx, (int) hy, (int) tip[0], (int) tip[1]);
        // guard (tsuba)
        g.setColor(Palette.GOLD);
        g.fillOval((int) (hx - 3 * s), (int) (hy - 3 * s), (int) (6 * s), (int) (6 * s));

        g.setStroke(old);
    }

    /** Returns {tipX, tipY, hiltX, hiltY} for a pose. */
    private static double[] swordTip(int pose, int facing, double s, double gx, double gy) {
        double hx = gx + facing * 6 * s, hy = gy;
        double tx, ty;
        switch (pose) {
            case POSE_GUARD_HIGH:   tx = hx + facing * 30 * s; ty = hy - 26 * s; break;
            case POSE_GUARD_LOW:    tx = hx + facing * 34 * s; ty = hy + 20 * s; break;
            case POSE_WINDUP:       hx = gx - facing * 6 * s; hy = gy - 10 * s;
                                    tx = hx - facing * 14 * s; ty = hy - 34 * s; break;
            case POSE_STRIKE_HIGH:  tx = hx + facing * 40 * s; ty = hy - 18 * s; break;
            case POSE_STRIKE_MID:   tx = hx + facing * 46 * s; ty = hy + 2 * s;  break;
            case POSE_STRIKE_LOW:   tx = hx + facing * 40 * s; ty = hy + 22 * s; break;
            case POSE_PARRY:        tx = hx + facing * 12 * s; ty = hy - 30 * s; break;
            case POSE_STAGGER:      tx = hx - facing * 8 * s;  ty = hy + 30 * s; break;
            default:                tx = hx + facing * 40 * s; ty = hy - 2 * s;  break; // guard mid
        }
        return new double[]{tx, ty, hx, hy};
    }

    // --- Top-down glyphs (MELEE / ninja engine, design ref §2c) ---

    /** Top-down hero: a body disc with a facing katana line. angle in radians. */
    public static void drawHero(Graphics2D g, int x, int y, int r, Color c, double angle) {
        g.setColor(new Color(0, 0, 0, 45));
        g.fillOval(x - r, y - r + 3, r * 2, r * 2);
        g.setColor(c);
        g.fillOval(x - r, y - r, r * 2, r * 2);
        g.setColor(Palette.INK);
        g.setStroke(new BasicStroke(2f));
        g.drawOval(x - r, y - r, r * 2, r * 2);
        // katana pointing forward
        int bx = (int) (x + Math.cos(angle) * r);
        int by = (int) (y + Math.sin(angle) * r);
        int tx = (int) (x + Math.cos(angle) * (r + 16));
        int ty = (int) (y + Math.sin(angle) * (r + 16));
        g.setColor(new Color(210, 216, 224));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(bx, by, tx, ty);
    }

    /** Top-down guard glyph; alerted guards are outlined in cinnabar. */
    public static void drawGuard(Graphics2D g, int x, int y, int r, boolean alerted, double angle) {
        g.setColor(alerted ? Palette.CINNABAR_DK : Palette.INK_SOFT);
        g.fillOval(x - r, y - r, r * 2, r * 2);
        g.setColor(alerted ? Palette.CINNABAR : Palette.DIM);
        g.setStroke(new BasicStroke(2f));
        g.drawOval(x - r, y - r, r * 2, r * 2);
        // spear/facing nub
        int tx = (int) (x + Math.cos(angle) * (r + 10));
        int ty = (int) (y + Math.sin(angle) * (r + 10));
        g.drawLine(x, y, tx, ty);
    }

    // --- Army glyphs (BATTLE engine, design ref §2b) ---

    public static final int UNIT_INFANTRY = 0;
    public static final int UNIT_ARCHER = 1;
    public static final int UNIT_CAVALRY = 2;
    public static final int UNIT_MUSKET = 3;

    /** A single battle unit-block glyph coloured by clan, shaped by unit type. */
    public static void drawSoldier(Graphics2D g, int x, int y, int size, Color clan, int type) {
        g.setColor(clan);
        switch (type) {
            case UNIT_CAVALRY: // diamond
                int[] px = {x, x + size, x, x - size};
                int[] py = {y - size, y, y + size, y};
                g.fillPolygon(px, py, 4);
                break;
            case UNIT_ARCHER:  // triangle
                int[] ax = {x, x + size, x - size};
                int[] ay = {y - size, y + size, y + size};
                g.fillPolygon(ax, ay, 3);
                break;
            case UNIT_MUSKET:  // square with a dark bore
                g.fillRect(x - size, y - size, size * 2, size * 2);
                g.setColor(Palette.INK);
                g.fillOval(x - 2, y - 2, 4, 4);
                break;
            default:           // infantry — filled circle
                g.fillOval(x - size, y - size, size * 2, size * 2);
        }
        g.setColor(Palette.INK);
        g.setStroke(new BasicStroke(1.5f));
        if (type == UNIT_INFANTRY) g.drawOval(x - size, y - size, size * 2, size * 2);
    }

    /** A small clan war-banner (nobori) glyph with a placeholder kanji stroke. */
    public static void drawBanner(Graphics2D g, int x, int y, Color clan, String label) {
        g.setColor(Palette.INK);
        g.setStroke(new BasicStroke(2f));
        g.drawLine(x, y, x, y - 34);           // pole
        g.setColor(clan);
        g.fillRect(x, y - 34, 16, 26);         // flag
        g.setColor(Palette.PAPER);
        g.drawLine(x + 8, y - 30, x + 8, y - 14); // placeholder mon stroke
        g.drawLine(x + 4, y - 22, x + 12, y - 22);
        if (label != null) {
            g.setColor(Palette.INK);
            g.drawString(label, x - 2, y + 12);
        }
    }
}
