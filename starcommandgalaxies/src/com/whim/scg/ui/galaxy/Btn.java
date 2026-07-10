package com.whim.scg.ui.galaxy;

import com.whim.scg.render.Palette;
import com.whim.scg.render.UiKit;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Tiny package-private clickable button record used by the galaxy + starport
 * screens. Rebuilt every frame from live view data; the screen keeps the current
 * list and hit-tests it on mouse press. Java 8: uses a Runnable action.
 */
final class Btn {
    final int x, y, w, h;
    final String label;
    final boolean enabled;
    final Runnable action;

    Btn(int x, int y, int w, int h, String label, boolean enabled, Runnable action) {
        this.x = x; this.y = y; this.w = w; this.h = h;
        this.label = label; this.enabled = enabled; this.action = action;
    }

    boolean hit(int px, int py) {
        return enabled && px >= x && px <= x + w && py >= y && py <= y + h;
    }

    void draw(Graphics2D g, int mx, int my) {
        boolean hot = enabled && hit(mx, my);
        Color face = !enabled ? Palette.GRID : (hot ? Palette.ACCENT : Palette.BG_PANEL);
        Color ink  = !enabled ? Palette.INK_DIM : (hot ? Palette.BG : Palette.INK);
        g.setColor(face);
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(Palette.GRID);
        g.drawRoundRect(x, y, w, h, 8, 8);
        UiKit.textCenter(g, label, x + w / 2, y + h / 2 + 5, UiKit.BODY, ink);
    }
}
