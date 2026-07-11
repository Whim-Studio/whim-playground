package com.whim.bc3k.app;

import com.whim.bc3k.api.Enums;
import com.whim.bc3k.api.GameController;
import com.whim.bc3k.api.Screen;
import com.whim.bc3k.render.Palette;
import com.whim.bc3k.render.Starfield;
import com.whim.bc3k.render.UiKit;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

/** Main menu: title + game-mode selection. Starts a session and enters the bridge. */
public final class MenuScreen implements Screen {
    private final GameController c;
    private final Starfield stars = new Starfield(220, 30L);
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
        UiKit.textCenter(g, "BATTLECRUISER 3000AD", cx, h / 2 - 150, UiKit.H1, Palette.ACCENT);
        UiKit.textCenter(g, "a clean-room Java 8 / Swing recreation", cx, h / 2 - 122, UiKit.BODY, Palette.INK_DIM);
        UiKit.textCenter(g, "Select a game mode:", cx, h / 2 - 78, UiKit.H2, Palette.INK);

        String[] keys = { "[1]", "[2]", "[3]" };
        Enums.GameMode[] modes = Enums.GameMode.values();
        int y = h / 2 - 40;
        for (int i = 0; i < modes.length; i++) {
            UiKit.textCenter(g, keys[i] + "  " + modes[i].title(), cx, y, UiKit.H2, Palette.INK);
            UiKit.textCenter(g, modes[i].blurb(), cx, y + 22, UiKit.BODY, Palette.INK_DIM);
            y += 62;
        }
        boolean canContinue = c.hasSave("auto");
        UiKit.textCenter(g, "[C] Continue (load autosave)", cx, y + 4,
                UiKit.BODY, canContinue ? Palette.INK : Palette.INK_DIM);
        UiKit.textCenter(g, "[Q] Quit", cx, y + 26, UiKit.BODY, Palette.INK_DIM);
        if (!note.isEmpty()) UiKit.textCenter(g, note, cx, y + 52, UiKit.BODY, Palette.ACCENT_WARM);
        UiKit.textCenter(g, "In the bridge: F1-F8 switch consoles - F9 save - P pauses - Esc returns here",
                cx, h - 40, UiKit.BODY, Palette.INK_DIM);
    }

    @Override public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_1: c.newGame(Enums.GameMode.FREE_FLIGHT, "GCV Whimsy"); break;
            case KeyEvent.VK_2: c.newGame(Enums.GameMode.XTREME_CARNAGE, "GCV Whimsy"); break;
            case KeyEvent.VK_3: c.newGame(Enums.GameMode.CAMPAIGN, "GCV Whimsy"); break;
            case KeyEvent.VK_C:
                note = c.load("auto").message();   // engine restores mode; shell follows view().mode()
                break;
            case KeyEvent.VK_Q: System.exit(0); break;
            default: break;
        }
    }
}
