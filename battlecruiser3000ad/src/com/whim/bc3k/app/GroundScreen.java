package com.whim.bc3k.app;

import com.whim.bc3k.api.Enums;
import com.whim.bc3k.api.GameController;
import com.whim.bc3k.api.Screen;
import com.whim.bc3k.api.Views;
import com.whim.bc3k.render.Palette;
import com.whim.bc3k.render.Starfield;
import com.whim.bc3k.render.UiKit;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

/** Planetary ground/ATV skirmish screen (reached from FLIGHT DECK: deploy ATV). */
public final class GroundScreen implements Screen {
    private final GameController c;
    private final Starfield stars = new Starfield(80, 17L);

    public GroundScreen(GameController c) { this.c = c; }

    @Override public Enums.Mode mode() { return Enums.Mode.GROUND; }
    @Override public void onEnter() {}
    @Override public void onExit() {}
    @Override public void update(double dt) { stars.update(dt); }

    @Override public void render(Graphics2D g, int w, int h) {
        UiKit.antialias(g);
        stars.render(g, w, h);
        UiKit.panel(g, 12, 12, w - 24, h - 24);
        int cx = w / 2;
        UiKit.textCenter(g, "PLANETARY GROUND ENGAGEMENT", cx, 60, UiKit.H1, Palette.ACCENT_WARM);

        Views.GroundView gr = c.view().ground();
        if (gr == null) {
            UiKit.textCenter(g, "No active engagement — deploy an ATV from the FLIGHT DECK (F8).",
                    cx, h / 2, UiKit.BODY, Palette.INK_DIM);
            return;
        }

        int bx = cx - 200, bw = 400;
        UiKit.text(g, "ATV DETACHMENT", bx, 150, UiKit.H2, Palette.HULL);
        UiKit.bar(g, bx, 162, bw, 20, gr.playerHp() / (double) gr.playerMaxHp(), Palette.GOOD, Palette.GRID);
        UiKit.text(g, gr.playerHp() + " / " + gr.playerMaxHp(), bx + bw + 12, 178, UiKit.MONO, Palette.INK);

        UiKit.text(g, "HOSTILE FORCE", bx, 230, UiKit.H2, Palette.BAD);
        UiKit.bar(g, bx, 242, bw, 20, gr.enemyHp() / (double) gr.enemyMaxHp(), Palette.BAD, Palette.GRID);
        UiKit.text(g, gr.enemyHp() + " / " + gr.enemyMaxHp(), bx + bw + 12, 258, UiKit.MONO, Palette.INK);

        if (gr.over()) {
            UiKit.textCenter(g, gr.playerWon() ? "LZ SECURED" : "DETACHMENT LOST",
                    cx, 330, UiKit.H1, gr.playerWon() ? Palette.GOOD : Palette.BAD);
            UiKit.textCenter(g, "Press F1-F8 to return to the bridge.", cx, 366, UiKit.BODY, Palette.INK_DIM);
        } else {
            UiKit.textCenter(g, "SPACE: order assault   -   forces also trade fire over time",
                    cx, 330, UiKit.BODY, Palette.ACCENT_WARM);
        }
    }

    @Override public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) c.assaultGround();
    }
}
