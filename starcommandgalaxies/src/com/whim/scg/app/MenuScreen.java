package com.whim.scg.app;

import com.whim.scg.api.ActionResult;
import com.whim.scg.api.Enums;
import com.whim.scg.api.GameController;
import com.whim.scg.api.Screen;
import com.whim.scg.render.Palette;
import com.whim.scg.render.Starfield;
import com.whim.scg.render.UiKit;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

/** Functional main menu (orchestrator-owned). */
public final class MenuScreen implements Screen {
    private final GameController c;
    private final Starfield stars = new Starfield(200, 42L);
    private String note = "";

    public MenuScreen(GameController c) { this.c = c; }

    @Override public Enums.Mode mode() { return Enums.Mode.MENU; }
    @Override public void onEnter() {}
    @Override public void onExit() {}
    @Override public void update(double dt) { stars.update(dt); }

    @Override public void render(Graphics2D g, int w, int h) {
        UiKit.antialias(g);
        stars.render(g, w, h);
        int cx = w / 2;
        UiKit.textCenter(g, "STAR COMMAND: GALAXIES", cx, h / 2 - 120, UiKit.H1, Palette.ACCENT);
        UiKit.textCenter(g, "a clean-room Java 8 / Swing recreation", cx, h / 2 - 92, UiKit.BODY, Palette.INK_DIM);

        String[] items = {
            "[N]  New Game",
            "[C]  Continue (load autosave)",
            "[H]  Help / Controls",
            "[Q]  Quit"
        };
        int y = h / 2 - 30;
        for (String s : items) {
            UiKit.textCenter(g, s, cx, y, UiKit.H2, Palette.INK);
            y += 40;
        }
        if (!note.isEmpty()) UiKit.textCenter(g, note, cx, y + 20, UiKit.BODY, Palette.ACCENT_WARM);
    }

    @Override public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_N:
                c.newGame("Cpt. Reyes", "SCV Whimsy");
                c.setMode(Enums.Mode.SHIP_INTERIOR);
                break;
            case KeyEvent.VK_C:
                ActionResult r = c.load("auto");
                note = r.isSuccess() ? "" : r.message();
                if (r.isSuccess()) c.setMode(Enums.Mode.SHIP_INTERIOR);
                break;
            case KeyEvent.VK_H:
                note = "Controls: drag crew to rooms - Space pauses combat - "
                     + "arrows/click to move - S saves - Esc returns here";
                break;
            case KeyEvent.VK_Q:
                System.exit(0);
                break;
            default: break;
        }
    }
}
