package com.whim.scg.ui.crew;

import com.whim.scg.api.Enums;
import com.whim.scg.api.Views;
import com.whim.scg.render.Palette;
import com.whim.scg.render.UiKit;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Shared crew-card drawing helpers used by both the ship-interior roster and the
 * boarding screen. Pure rendering — no game state kept here. Task 3 owned.
 */
public final class CrewCard {
    private CrewCard() {}

    /** Preferred size for a roster card. */
    public static final int W = 250;
    public static final int H = 78;

    /** Short label for a crew role (invented flavour titles). */
    public static String roleTitle(Enums.CrewRole r) {
        if (r == null) return "Crew";
        switch (r) {
            case CAPTAIN:     return "Captain";
            case PILOT:       return "Helm Pilot";
            case ENGINEER:    return "Engineer";
            case GUNNER:      return "Gunner";
            case SHIELD_TECH: return "Shield Tech";
            case MEDIC:       return "Medic";
            case SECURITY:    return "Marine";
            case SCIENCE:     return "Science Officer";
            default:          return "Crew";
        }
    }

    /** Colour for a happiness value. */
    public static Color moodColor(int happiness) {
        if (happiness >= 66) return Palette.GOOD;
        if (happiness >= 33) return Palette.ACCENT_WARM;
        return Palette.BAD;
    }

    /** Draw a small round crew token (used on the ship grid + boarding grid). */
    public static void token(Graphics2D g, Views.CrewView cv, int cx, int cy, int r, boolean selected) {
        Color base = cv == null ? Palette.INK_DIM : Palette.faction(cv.faction());
        if (cv != null && !cv.alive()) base = Palette.BREACH;
        g.setColor(base.darker());
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(base);
        g.fillOval(cx - r + 2, cy - r + 2, r * 2 - 4, r * 2 - 4);
        if (selected) {
            g.setColor(Palette.INK);
            g.drawOval(cx - r - 2, cy - r - 2, r * 2 + 4, r * 2 + 4);
        }
        // initial
        String ini = cv == null || cv.name() == null || cv.name().isEmpty()
                ? "?" : cv.name().substring(0, 1).toUpperCase();
        UiKit.textCenter(g, ini, cx, cy + 4, UiKit.BODY, Palette.BG);
    }

    /**
     * Draw a full roster card. {@code hot} highlights it (hover/selected).
     * Renders name, role, level/xp, HP + happiness bars and the primary skill.
     */
    public static void roster(Graphics2D g, Views.CrewView cv, int x, int y, int w, int h,
                              boolean hot) {
        g.setColor(hot ? Palette.GRID : Palette.BG_PANEL);
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(hot ? Palette.ACCENT : Palette.GRID);
        g.drawRoundRect(x, y, w, h, 10, 10);
        if (cv == null) return;

        int r = 15;
        token(g, cv, x + 4 + r, y + 4 + r, r, false);

        int tx = x + 8 + r * 2 + 6;
        String name = cv.name() == null ? "—" : cv.name();
        UiKit.text(g, name, tx, y + 20, UiKit.H2, cv.alive() ? Palette.INK : Palette.INK_DIM);
        UiKit.text(g, roleTitle(cv.role()) + "   Lv " + cv.level(), tx, y + 38, UiKit.BODY, Palette.INK_DIM);

        // bars
        int barX = tx;
        int barW = x + w - barX - 12;
        int hpY = y + 48;
        double hpFrac = cv.maxHp() > 0 ? (double) cv.hp() / cv.maxHp() : 0;
        UiKit.bar(g, barX, hpY, barW, 6, hpFrac, Palette.GOOD, Palette.BREACH);
        UiKit.text(g, "HP", barX - 0, hpY - 0, UiKit.MONO, Palette.INK_DIM);

        int hapY = y + 60;
        UiKit.bar(g, barX, hapY, barW, 6, cv.happiness() / 100.0, moodColor(cv.happiness()), Palette.BREACH);

        // primary skill number, right aligned in header
        Enums.StatType prim = cv.role() == null ? null : cv.role().primary();
        if (prim != null) {
            String sk = prim.name().substring(0, 3) + " " + cv.skill(prim);
            g.setFont(UiKit.MONO);
            int sw = g.getFontMetrics().stringWidth(sk);
            UiKit.text(g, sk, x + w - sw - 10, y + 20, UiKit.MONO, Palette.ACCENT);
        }
    }
}
