package com.whim.settlers.ui;

import com.whim.settlers.buildings.Building;
import com.whim.settlers.engine.World;
import com.whim.settlers.military.MilitarySystem;
import com.whim.settlers.military.Players;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Bottom military bar: always shows the knight-conversion target and default
 * rank (the player's standing knobs), and — when an enemy fort is selected —
 * an attack sub-panel to choose how many knights to send and launch the assault.
 * Buttons are recorded during {@link #render} and dispatched by {@link #handleClick}.
 */
public final class MilitaryPanel {

    private static final int H = 64;
    private static final int BTN = 16;

    private final Font header = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    private final Font body = new Font(Font.SANS_SERIF, Font.PLAIN, 11);

    private Building target;         // selected enemy fort, or null
    private int sendCount = 3;
    private final List<Hit> hits = new ArrayList<Hit>();

    public void setTarget(Building b) {
        this.target = b;
        if (b != null) this.sendCount = 3;
    }
    public Building target() { return target; }

    public Rectangle bounds(int viewportW, int viewportH) {
        int x = BuildMenu.WIDTH;
        return new Rectangle(x, viewportH - H, viewportW - x, H);
    }

    public boolean contains(int x, int y, int viewportW, int viewportH) {
        return bounds(viewportW, viewportH).contains(x, y);
    }

    public void render(Graphics2D g, final World world, int viewportW, int viewportH) {
        hits.clear();
        final MilitarySystem mil = world.military();
        int x0 = BuildMenu.WIDTH;
        int y0 = viewportH - H;
        g.setColor(new Color(0, 0, 0, 175));
        g.fillRect(x0, y0, viewportW - x0, H);
        g.setColor(new Color(255, 255, 255, 40));
        g.drawLine(x0, y0, viewportW, y0);

        int x = x0 + 12, y = y0 + 20;
        g.setFont(header);
        g.setColor(new Color(0x9FC0F0));
        g.drawString("MILITARY", x, y);
        g.setFont(body);
        g.setColor(new Color(220, 220, 220));
        g.drawString("Knights: " + mil.knightCount(Players.HUMAN), x + 80, y);

        int cy = y0 + 38;
        g.setColor(new Color(210, 210, 210));
        g.drawString("Knight target", x, cy + 11);
        int bx = x + 90;
        button(g, bx, cy, "−", () -> mil.bumpKnightTarget(-1));
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(mil.knightTarget()), bx + BTN + 6, cy + 12);
        button(g, bx + 2 * BTN, cy, "+", () -> mil.bumpKnightTarget(1));

        g.setColor(new Color(210, 210, 210));
        g.drawString("New rank", bx + 3 * BTN + 24, cy + 11);
        int rx = bx + 3 * BTN + 90;
        button(g, rx, cy, "−", () -> mil.bumpDefaultRank(-1));
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(mil.defaultRank()), rx + BTN + 6, cy + 12);
        button(g, rx + 2 * BTN, cy, "+", () -> mil.bumpDefaultRank(1));

        if (target != null) drawAttack(g, world, mil, viewportW, y0);
    }

    private void drawAttack(Graphics2D g, final World world, final MilitarySystem mil,
                            int viewportW, int y0) {
        final Building t = target;
        int px = viewportW - 300;
        int y = y0 + 20;
        g.setColor(new Color(0xD9534A));
        g.setFont(header);
        g.drawString("ATTACK " + t.type().displayName(), px, y);
        g.setFont(body);
        g.setColor(new Color(230, 220, 220));
        g.drawString("Defenders: " + mil.garrisonSize(t)
                + "   Your knights: " + mil.knightCount(Players.HUMAN), px, y0 + 36);

        int cy = y0 + 44;
        int bx = px;
        g.setColor(new Color(210, 210, 210));
        g.drawString("Send", bx, cy + 11);
        bx += 40;
        button(g, bx, cy, "−", () -> sendCount = Math.max(1, sendCount - 1));
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(sendCount), bx + BTN + 6, cy + 12);
        button(g, bx + 2 * BTN, cy, "+", () -> sendCount = sendCount + 1);

        int ax = bx + 3 * BTN + 12;
        g.setColor(new Color(0xB5433A));
        g.fillRect(ax, cy - 2, 62, BTN + 4);
        g.setColor(Color.WHITE);
        g.drawString("Attack", ax + 8, cy + 12);
        hits.add(new Hit(new Rectangle(ax, cy - 2, 62, BTN + 4), () -> {
            world.military().launchAttack(t, sendCount);
            target = null;
        }));

        int xx = viewportW - 22;
        g.setColor(new Color(120, 120, 120));
        g.drawString("[x]", xx - 8, y);
        hits.add(new Hit(new Rectangle(xx - 10, y - 12, 24, 16), () -> target = null));
    }

    private void button(Graphics2D g, int x, int y, String label, Runnable action) {
        g.setColor(new Color(70, 80, 95));
        g.fillRect(x, y, BTN, BTN);
        g.setColor(new Color(200, 210, 220));
        g.drawRect(x, y, BTN, BTN);
        g.drawString(label, x + 4, y + 12);
        hits.add(new Hit(new Rectangle(x, y, BTN, BTN), action));
    }

    /** Returns true if a button was hit. */
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
