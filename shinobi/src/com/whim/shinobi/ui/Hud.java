package com.whim.shinobi.ui;

import com.whim.shinobi.api.Config;
import com.whim.shinobi.api.Enums;
import com.whim.shinobi.api.Views;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Classic arcade HUD. A top bar shows SCORE / WEAPON / TIME; a bottom bar shows
 * LIVES / NINJUTSU / HOSTAGES. Big pixel-ish glyphs come from a monospaced bold
 * {@link Font}; small ninjutsu/life icons are drawn procedurally. The playfield is
 * rendered into the {@link Config#VIEW_H}-tall band between the two bars.
 */
public final class Hud {

    public static final int TOP_H = 34;
    public static final int BOTTOM_H = 30;
    /** Total window content height = HUD bars + playfield viewport. */
    public static final int TOTAL_H = TOP_H + Config.VIEW_H + BOTTOM_H;

    private final Font big = new Font(Font.MONOSPACED, Font.BOLD, 18);
    private final Font small = new Font(Font.MONOSPACED, Font.BOLD, 13);

    /** Draw the top bar (already translated to window origin 0,0). */
    public void drawTop(Graphics2D g, Views.GameStateView state) {
        g.setColor(Palette.HUD_BG);
        g.fillRect(0, 0, Config.VIEW_W, TOP_H);
        g.setColor(new Color(60, 60, 80));
        g.fillRect(0, TOP_H - 2, Config.VIEW_W, 2);

        Views.PlayerView p = state.player();
        g.setFont(big);

        label(g, "1UP", 8, 13, Palette.HUD_RED);
        text(g, pad(p.score(), 6), 8, 28, Palette.HUD_TEXT);

        String wpn = weaponName(p.weapon());
        label(g, "WPN", 176, 13, Palette.HUD_LABEL);
        text(g, wpn, 176, 28, Palette.HUD_TEXT);

        int t = Math.max(0, state.secondsRemaining());
        Color timeCol = t <= 15 ? Palette.HUD_RED : Palette.HUD_TEXT;
        label(g, "TIME", Config.VIEW_W - 96, 13, Palette.HUD_LABEL);
        text(g, pad(t, 3), Config.VIEW_W - 60, 28, timeCol);
    }

    /** Draw the bottom bar (already translated to its window origin). */
    public void drawBottom(Graphics2D g, Views.GameStateView state) {
        g.setColor(Palette.HUD_BG);
        g.fillRect(0, 0, Config.VIEW_W, BOTTOM_H);
        g.setColor(new Color(60, 60, 80));
        g.fillRect(0, 0, Config.VIEW_W, 2);

        Views.PlayerView p = state.player();
        g.setFont(small);

        // Lives as little Joe heads
        text(g, "LIVES", 8, 20, Palette.HUD_LABEL);
        int lx = 62;
        for (int i = 0; i < Math.min(p.lives(), 5); i++) {
            drawLifeIcon(g, lx + i * 16, 8);
        }
        if (p.lives() > 5) text(g, "x" + p.lives(), lx + 5 * 16, 20, Palette.HUD_TEXT);

        // Ninjutsu icons
        text(g, "NINJUTSU", 176, 20, Palette.HUD_LABEL);
        int nx = 256;
        for (int i = 0; i < Math.min(p.ninjutsu(), 6); i++) {
            drawNinjutsuIcon(g, nx + i * 16, 8);
        }

        // Hostages rescued/total
        text(g, "HOSTAGES", Config.VIEW_W - 150, 20, Palette.HUD_LABEL);
        text(g, state.hostagesRescued() + "/" + state.hostagesTotal(),
                Config.VIEW_W - 52, 20, Palette.HOSTAGE_FREED);
    }

    /** Center overlay text for paused / clear / game over phases. */
    public void drawPhaseOverlay(Graphics2D g, Views.GameStateView state) {
        Enums.Phase ph = state.phase();
        String msg = null;
        Color col = Palette.HUD_TEXT;
        if (ph == Enums.Phase.PAUSED) { msg = "PAUSED"; }
        else if (ph == Enums.Phase.LEVEL_CLEAR) { msg = "LEVEL CLEAR"; col = Palette.HOSTAGE_FREED; }
        else if (ph == Enums.Phase.GAME_OVER) { msg = "GAME OVER"; col = Palette.HUD_RED; }
        if (msg == null) return;

        g.setColor(Palette.OVERLAY);
        g.fillRect(0, 0, Config.VIEW_W, Config.VIEW_H);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 40));
        int w = g.getFontMetrics().stringWidth(msg);
        text(g, msg, (Config.VIEW_W - w) / 2, Config.VIEW_H / 2, col);
    }

    // ------------------------------------------------------------- icon glyphs

    private void drawLifeIcon(Graphics2D g, int x, int y) {
        g.setColor(Palette.JOE_SUIT);
        g.fillOval(x, y, 12, 12);
        g.setColor(Palette.JOE_TRIM);
        g.fillRect(x + 1, y + 5, 10, 3); // red headband
        g.setColor(Palette.JOE_SKIN);
        g.fillRect(x + 7, y + 6, 3, 2);
    }

    private void drawNinjutsuIcon(Graphics2D g, int x, int y) {
        g.setColor(Palette.HUD_NINJUTSU);
        int cx = x + 6, cy = y + 6, r = 6;
        int[] xs = { cx, cx + r, cx, cx - r };
        int[] ys = { cy - r, cy, cy + r, cy };
        g.fillPolygon(xs, ys, 4);
        g.setColor(new Color(30, 40, 90));
        g.fillOval(cx - 2, cy - 2, 4, 4);
    }

    // --------------------------------------------------------------- text util

    private void label(Graphics2D g, String s, int x, int y, Color c) {
        Font f = g.getFont();
        g.setFont(small);
        text(g, s, x, y, c);
        g.setFont(f);
    }

    private void text(Graphics2D g, String s, int x, int y, Color c) {
        g.setColor(new Color(0, 0, 0, 160));
        g.drawString(s, x + 1, y + 1);
        g.setColor(c);
        g.drawString(s, x, y);
    }

    private static String weaponName(Enums.Weapon w) {
        switch (w) {
            case KNIFE: return "KNIFE";
            case GUN: return "GUN";
            default: return "SHURIKEN";
        }
    }

    private static String pad(int v, int width) {
        String s = Integer.toString(v);
        StringBuilder sb = new StringBuilder();
        for (int i = s.length(); i < width; i++) sb.append('0');
        sb.append(s);
        return sb.toString();
    }
}
