package com.whim.settlers.ui;

import com.whim.settlers.engine.Game;
import com.whim.settlers.engine.SetupConfig;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen meta UI for the non-playing states: the main menu, the new-game /
 * free-play setup screen (map, player count, per-AI personality slider), and the
 * victory / defeat end screens. Clickable regions are recorded during {@link
 * #render} and dispatched by {@link #handleClick}, so drawing and hit-testing
 * share one geometry — the same pattern the in-game panels use.
 */
public final class MetaScreen {

    private static final Color BG    = new Color(0x12, 0x1a, 0x22);
    private static final Color PANEL = new Color(0x1e, 0x2a, 0x36);
    private static final Color ACCENT= new Color(0x6F, 0xB0, 0xE0);

    private final Font title = new Font(Font.SANS_SERIF, Font.BOLD, 44);
    private final Font h2    = new Font(Font.SANS_SERIF, Font.BOLD, 20);
    private final Font body  = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    private final Font btnF  = new Font(Font.SANS_SERIF, Font.BOLD, 15);

    private final List<Hit> hits = new ArrayList<Hit>();

    public void render(Graphics2D g, Game game, int vw, int vh) {
        hits.clear();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        switch (game.state()) {
            case MENU:
                g.setColor(BG); g.fillRect(0, 0, vw, vh);
                renderMenu(g, game, vw, vh);
                break;
            case SETUP:
                g.setColor(BG); g.fillRect(0, 0, vw, vh);
                renderSetup(g, game, vw, vh);
                break;
            case VICTORY:
                // Drawn over the final board (Renderer draws the world first).
                g.setColor(new Color(0, 0, 0, 170)); g.fillRect(0, 0, vw, vh);
                renderEnd(g, game, vw, vh, true);
                break;
            case DEFEAT:
                g.setColor(new Color(0, 0, 0, 170)); g.fillRect(0, 0, vw, vh);
                renderEnd(g, game, vw, vh, false);
                break;
            default: break;
        }
    }

    // ------------------------------------------------------------------- menu

    private void renderMenu(Graphics2D g, final Game game, int vw, int vh) {
        int cx = vw / 2;
        g.setFont(title);
        g.setColor(Color.WHITE);
        centered(g, "THE SETTLERS", cx, vh / 2 - 120);
        g.setFont(body);
        g.setColor(new Color(180, 190, 200));
        centered(g, "A clean-room Java 8 / Swing recreation", cx, vh / 2 - 88);

        int by = vh / 2 - 30, bw = 240, bh = 44, gap = 14;
        button(g, cx - bw / 2, by, bw, bh, "New Game", new Runnable() {
            public void run() { game.openSetup(); }
        });
        button(g, cx - bw / 2, by + (bh + gap), bw, bh, "Quick Start", new Runnable() {
            public void run() { game.quickStart(); }
        });
        button(g, cx - bw / 2, by + 2 * (bh + gap), bw, bh, "Quit", new Runnable() {
            public void run() { System.exit(0); }
        });
        g.setFont(body);
        g.setColor(new Color(140, 150, 160));
        centered(g, "Every game begins by placing your Castle.", cx, by + 3 * (bh + gap) + 24);
    }

    // ------------------------------------------------------------------ setup

    private void renderSetup(Graphics2D g, final Game game, int vw, int vh) {
        final SetupConfig cfg = game.config();
        int cx = vw / 2;
        int panelW = 460, panelH = 380;
        int px = cx - panelW / 2, py = vh / 2 - panelH / 2;
        g.setColor(PANEL);
        g.fillRoundRect(px, py, panelW, panelH, 14, 14);
        g.setColor(new Color(255, 255, 255, 30));
        g.drawRoundRect(px, py, panelW, panelH, 14, 14);

        g.setFont(h2);
        g.setColor(Color.WHITE);
        centered(g, "NEW GAME", cx, py + 34);

        int x = px + 28;
        int y = py + 72;
        g.setFont(body);

        // Map choice.
        label(g, "Map", x, y);
        toggle(g, x + 90, y - 15, 130, "Generated", !cfg.tutorialMap(), new Runnable() {
            public void run() { cfg.setTutorialMap(false); }
        });
        toggle(g, x + 228, y - 15, 150, "Tutorial Valley", cfg.tutorialMap(), new Runnable() {
            public void run() { cfg.setTutorialMap(true); }
        });
        y += 40;

        // Seed (only meaningful for a generated map).
        if (!cfg.tutorialMap()) {
            label(g, "Seed", x, y);
            stepper(g, x + 90, y - 15, String.valueOf(cfg.seed()),
                    new Runnable() { public void run() { cfg.bumpSeed(-1); } },
                    new Runnable() { public void run() { cfg.bumpSeed(1); } });
            y += 40;
        }

        // Player count.
        label(g, "Players", x, y);
        stepper(g, x + 90, y - 15, cfg.players() + "  (1 you + " + cfg.aiCount() + " AI)",
                new Runnable() { public void run() { cfg.bumpPlayers(-1); } },
                new Runnable() { public void run() { cfg.bumpPlayers(1); } });
        y += 44;

        // Per-AI personality sliders.
        g.setFont(body);
        g.setColor(ACCENT);
        g.drawString("AI personality", x, y);
        y += 22;
        for (int i = 0; i < cfg.aiCount(); i++) {
            personalityRow(g, cfg, i, x, y, panelW - 56);
            y += 34;
        }

        if (game.setupError() != null) {
            g.setColor(new Color(0xE0, 0x70, 0x70));
            g.setFont(body);
            centered(g, game.setupError(), cx, py + panelH - 66);
        }

        int by = py + panelH - 48;
        button(g, cx - 165, by, 150, 36, "Back", new Runnable() {
            public void run() { game.openMenu(); }
        });
        button(g, cx + 15, by, 150, 36, "Start Game", new Runnable() {
            public void run() { game.startGame(); }
        });
    }

    private void personalityRow(Graphics2D g, final SetupConfig cfg, final int ai,
                                int x, int y, int rowW) {
        g.setFont(body);
        g.setColor(new Color(210, 215, 220));
        g.drawString("AI " + (ai + 1), x, y + 12);

        int barX = x + 52, barW = rowW - 190, barY = y + 4, barH = 8;
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(barX, barY, barW, barH, 6, 6);
        float v = cfg.aggression(ai);
        g.setColor(new Color(0x60 + (int) (0x80 * v), 0xB0 - (int) (0x50 * v), 0x70));
        g.fillRoundRect(barX, barY, (int) (barW * v), barH, 6, 6);
        int knobX = barX + (int) (barW * v);
        g.setColor(Color.WHITE);
        g.fillOval(knobX - 5, barY - 2, 12, 12);

        stepper(g, barX + barW + 8, y - 11, "",
                new Runnable() { public void run() { cfg.bumpAggression(ai, -0.1f); } },
                new Runnable() { public void run() { cfg.bumpAggression(ai, 0.1f); } });
        g.setFont(body);
        g.setColor(new Color(200, 210, 220));
        g.drawString(SetupConfig.personalityLabel(v), barX + barW + 66, y + 12);
    }

    // -------------------------------------------------------------------- end

    private void renderEnd(Graphics2D g, final Game game, int vw, int vh, boolean win) {
        int cx = vw / 2;
        g.setFont(title);
        g.setColor(win ? new Color(0x7F, 0xE0, 0x8F) : new Color(0xE0, 0x6F, 0x6F));
        centered(g, win ? "VICTORY" : "DEFEAT", cx, vh / 2 - 60);
        g.setFont(body);
        g.setColor(new Color(200, 205, 210));
        centered(g, win ? "Every rival has been eliminated. The land is yours."
                        : "Your Castle has fallen. The realm is lost.",
                 cx, vh / 2 - 20);

        int bw = 200, bh = 42;
        button(g, cx - bw / 2, vh / 2 + 20, bw, bh, "Play Again", new Runnable() {
            public void run() { game.openSetup(); }
        });
        button(g, cx - bw / 2, vh / 2 + 20 + bh + 12, bw, bh, "Main Menu", new Runnable() {
            public void run() { game.openMenu(); }
        });
    }

    // ---------------------------------------------------------------- widgets

    private void label(Graphics2D g, String s, int x, int y) {
        g.setFont(body);
        g.setColor(new Color(210, 215, 220));
        g.drawString(s, x, y);
    }

    private void button(Graphics2D g, int x, int y, int w, int h, String text, Runnable action) {
        g.setColor(new Color(0x2c, 0x3e, 0x50));
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(ACCENT);
        g.drawRoundRect(x, y, w, h, 10, 10);
        g.setFont(btnF);
        g.setColor(Color.WHITE);
        int tw = g.getFontMetrics().stringWidth(text);
        g.drawString(text, x + (w - tw) / 2, y + h / 2 + 5);
        hits.add(new Hit(new Rectangle(x, y, w, h), action));
    }

    private void toggle(Graphics2D g, int x, int y, int w, String text, boolean on, Runnable action) {
        int h = 26;
        g.setColor(on ? new Color(0x2c, 0x5e, 0x88) : new Color(0x24, 0x30, 0x3a));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(on ? ACCENT : new Color(90, 100, 110));
        g.drawRoundRect(x, y, w, h, 8, 8);
        g.setFont(body);
        g.setColor(on ? Color.WHITE : new Color(170, 180, 190));
        int tw = g.getFontMetrics().stringWidth(text);
        g.drawString(text, x + (w - tw) / 2, y + 17);
        hits.add(new Hit(new Rectangle(x, y, w, h), action));
    }

    /** A "− value +" stepper. If {@code value} is empty, only the two buttons show. */
    private void stepper(Graphics2D g, int x, int y, String value, Runnable minus, Runnable plus) {
        int b = 26;
        smallBtn(g, x, y, b, "−", minus);
        int valW = 0;
        if (!value.isEmpty()) {
            g.setFont(body);
            g.setColor(Color.WHITE);
            valW = Math.max(40, g.getFontMetrics().stringWidth(value) + 16);
            g.drawString(value, x + b + 8, y + 18);
        }
        smallBtn(g, x + b + valW + (value.isEmpty() ? 6 : 8), y, b, "+", plus);
    }

    private void smallBtn(Graphics2D g, int x, int y, int b, String text, Runnable action) {
        g.setColor(new Color(0x35, 0x48, 0x5c));
        g.fillRoundRect(x, y, b, b, 6, 6);
        g.setColor(new Color(150, 170, 190));
        g.drawRoundRect(x, y, b, b, 6, 6);
        g.setFont(btnF);
        g.setColor(Color.WHITE);
        g.drawString(text, x + b / 2 - 4, y + b / 2 + 6);
        hits.add(new Hit(new Rectangle(x, y, b, b), action));
    }

    private void centered(Graphics2D g, String s, int cx, int y) {
        int w = g.getFontMetrics().stringWidth(s);
        g.drawString(s, cx - w / 2, y);
    }

    /** Dispatch a click; returns true if it hit a control. */
    public boolean handleClick(int mx, int my) {
        for (Hit h : hits) {
            if (h.rect.contains(mx, my)) { h.action.run(); return true; }
        }
        return false;
    }

    private static final class Hit {
        final Rectangle rect;
        final Runnable action;
        Hit(Rectangle rect, Runnable action) { this.rect = rect; this.action = action; }
    }
}
