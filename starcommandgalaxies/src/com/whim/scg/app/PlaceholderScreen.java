package com.whim.scg.app;

import com.whim.scg.api.Enums;
import com.whim.scg.api.Screen;
import com.whim.scg.render.Palette;
import com.whim.scg.render.Starfield;
import com.whim.scg.render.UiKit;

import java.awt.Graphics2D;

/** Shown when a mode's real screen has not been built/loaded yet. */
public final class PlaceholderScreen implements Screen {
    private final Enums.Mode mode;
    private final String className;
    private final Starfield stars = new Starfield(120, 7L);

    public PlaceholderScreen(Enums.Mode mode, String className) {
        this.mode = mode; this.className = className;
    }

    @Override public Enums.Mode mode() { return mode; }
    @Override public void onEnter() {}
    @Override public void onExit() {}
    @Override public void update(double dt) { stars.update(dt); }

    @Override public void render(Graphics2D g, int w, int h) {
        UiKit.antialias(g);
        stars.render(g, w, h);
        UiKit.textCenter(g, mode.name() + " screen", w / 2, h / 2 - 20, UiKit.H1, Palette.INK);
        UiKit.textCenter(g, "not yet implemented — expected class:", w / 2, h / 2 + 12, UiKit.BODY, Palette.INK_DIM);
        UiKit.textCenter(g, className, w / 2, h / 2 + 34, UiKit.MONO, Palette.ACCENT);
        UiKit.textCenter(g, "Press M for main menu", w / 2, h / 2 + 70, UiKit.BODY, Palette.INK_DIM);
    }

    @Override public void keyPressed(java.awt.event.KeyEvent e) {}
}
