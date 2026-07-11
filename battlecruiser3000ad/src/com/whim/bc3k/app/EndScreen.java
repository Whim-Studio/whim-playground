package com.whim.bc3k.app;

import com.whim.bc3k.api.Enums;
import com.whim.bc3k.api.GameController;
import com.whim.bc3k.api.Screen;
import com.whim.bc3k.render.Palette;
import com.whim.bc3k.render.Starfield;
import com.whim.bc3k.render.UiKit;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

/** Terminal screen (game over / mission end). Phase 2 stub — reachable in later phases. */
public final class EndScreen implements Screen {
    private final GameController c;
    private final Starfield stars = new Starfield(140, 5L);

    public EndScreen(GameController c) { this.c = c; }

    @Override public Enums.Mode mode() { return Enums.Mode.GAME_OVER; }
    @Override public void onEnter() {}
    @Override public void onExit() {}
    @Override public void update(double dt) { stars.update(dt); }

    @Override public void render(Graphics2D g, int w, int h) {
        UiKit.antialias(g);
        stars.render(g, w, h);
        UiKit.textCenter(g, "MISSION END", w / 2, h / 2 - 10, UiKit.H1, Palette.BAD);
        UiKit.textCenter(g, "Press M for the main menu", w / 2, h / 2 + 24, UiKit.BODY, Palette.INK_DIM);
    }

    @Override public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_M) c.setMode(Enums.Mode.MENU);
    }
}
