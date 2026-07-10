package com.whim.scg.app;

import com.whim.scg.api.Enums;
import com.whim.scg.api.GameController;
import com.whim.scg.api.Screen;
import com.whim.scg.render.Palette;
import com.whim.scg.render.Starfield;
import com.whim.scg.render.UiKit;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

/** Shared game-over / victory screen. */
public final class EndScreen implements Screen {
    private final GameController c;
    private final Starfield stars = new Starfield(150, 99L);

    public EndScreen(GameController c) { this.c = c; }

    // Registered for both GAME_OVER and VICTORY; report whichever is current.
    @Override public Enums.Mode mode() { return c.view().mode(); }
    @Override public void onEnter() {}
    @Override public void onExit() {}
    @Override public void update(double dt) { stars.update(dt); }

    @Override public void render(Graphics2D g, int w, int h) {
        UiKit.antialias(g);
        stars.render(g, w, h);
        boolean win = c.view().mode() == Enums.Mode.VICTORY;
        UiKit.textCenter(g, win ? "VICTORY" : "GAME OVER", w / 2, h / 2 - 10,
                UiKit.H1, win ? Palette.GOOD : Palette.BAD);
        UiKit.textCenter(g, "Day " + c.view().day() + " - " + c.view().credits() + " credits",
                w / 2, h / 2 + 22, UiKit.BODY, Palette.INK_DIM);
        UiKit.textCenter(g, "Press Enter for the main menu", w / 2, h / 2 + 56, UiKit.BODY, Palette.INK);
    }

    @Override public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) c.setMode(Enums.Mode.MENU);
    }
}
