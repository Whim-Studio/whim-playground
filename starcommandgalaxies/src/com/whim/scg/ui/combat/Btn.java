package com.whim.scg.ui.combat;

import com.whim.scg.render.Palette;
import com.whim.scg.render.UiKit;

import java.awt.Color;
import java.awt.Graphics2D;

/** Package-private clickable button for the space-combat screen (Java 8 Runnable action). */
final class Btn {
    final int x, y, w, h;
    final String label;
    final boolean enabled;
    final boolean primary;
    final Runnable action;

    Btn(int x, int y, int w, int h, String label, boolean enabled, boolean primary, Runnable action) {
        this.x = x; this.y = y; this.w = w; this.h = h;
        this.label = label; this.enabled = enabled; this.primary = primary; this.action = action;
    }

    boolean hit(int px, int py) {
        return enabled && px >= x && px <= x + w && py >= y && py <= y + h;
    }

    void draw(Graphics2D g, int mx, int my) {
        if (label.isEmpty()) return; // invisible hotspot (e.g. weapon-row select)
        boolean hot = enabled && hit(mx, my);
        Color face;
        Color ink;
        if (!enabled) { face = Palette.GRID; ink = Palette.INK_DIM; }
        else if (hot) { face = Palette.ACCENT; ink = Palette.BG; }
        else if (primary) { face = Palette.ACCENT_WARM; ink = Palette.BG; }
        else { face = Palette.BG_PANEL; ink = Palette.INK; }
        g.setColor(face);
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(Palette.GRID);
        g.drawRoundRect(x, y, w, h, 8, 8);
        UiKit.textCenter(g, label, x + w / 2, y + h / 2 + 5, UiKit.BODY, ink);
    }
}
