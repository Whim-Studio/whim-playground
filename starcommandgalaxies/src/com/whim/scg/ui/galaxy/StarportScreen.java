package com.whim.scg.ui.galaxy;

import com.whim.scg.api.ActionResult;
import com.whim.scg.api.Enums;
import com.whim.scg.api.GameController;
import com.whim.scg.api.Screen;
import com.whim.scg.api.Views;
import com.whim.scg.render.Palette;
import com.whim.scg.render.Starfield;
import com.whim.scg.render.UiKit;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * STARPORT — dock services. Repair the ship, recruit crew, upgrade the hull, and
 * buy tech-track levels from {@code view().techTree()} via {@code buyTech(type)}.
 * Undock returns to the galaxy map. Null-safe against the stub controller.
 */
public final class StarportScreen implements Screen {

    private final GameController c;
    private final Starfield stars = new Starfield(120, 23L);
    private int mx, my;
    private String note = "";
    private final List<Btn> buttons = new ArrayList<Btn>();

    public StarportScreen(GameController c) { this.c = c; }

    @Override public Enums.Mode mode() { return Enums.Mode.STARPORT; }
    @Override public void onEnter() { note = ""; }
    @Override public void onExit() {}
    @Override public void update(double dt) { stars.update(dt); }

    @Override public void render(Graphics2D g, int w, int h) {
        UiKit.antialias(g);
        stars.render(g, w, h);
        buttons.clear();

        Views.GameView v = c.view();
        Views.ShipView ship = v == null ? null : v.playerShip();

        UiKit.text(g, "STARPORT", 24, 44, UiKit.H1, Palette.ACCENT);
        if (v != null)
            UiKit.text(g, "Credits " + v.credits() + "   ·   Day " + v.day(),
                    24, 74, UiKit.H2, Palette.INK);

        // undock always available
        buttons.add(new Btn(w - 150, 30, 126, 36, "Undock", true, new Runnable() {
            public void run() { note = msg(c.undock()); }
        }));

        if (ship == null) {
            UiKit.panel(g, 24, 96, w - 48, 200);
            UiKit.textCenter(g, "AWAITING ENGINE", w / 2, 190, UiKit.H2, Palette.INK_DIM);
            UiKit.textCenter(g, "dock services unavailable — no ship data",
                    w / 2, 216, UiKit.BODY, Palette.INK_DIM);
            for (Btn b : buttons) b.draw(g, mx, my);
            return;
        }

        // ---- left: ship + services ----
        int lx = 24, ly = 96, lw = 380;
        UiKit.panel(g, lx, ly, lw, h - ly - 30);
        int tx = lx + 18, ty = ly + 32;
        UiKit.text(g, ship.name(), tx, ty, UiKit.H2, Palette.INK); ty += 30;

        UiKit.text(g, "Hull", tx, ty, UiKit.BODY, Palette.INK_DIM);
        UiKit.bar(g, tx + 70, ty - 11, 200, 12, frac(ship.hull(), ship.maxHull()),
                Palette.HULL, Palette.GRID);
        UiKit.text(g, ship.hull() + "/" + ship.maxHull(), tx + 282, ty, UiKit.BODY, Palette.INK);
        ty += 24;
        UiKit.text(g, "Shield", tx, ty, UiKit.BODY, Palette.INK_DIM);
        UiKit.bar(g, tx + 70, ty - 11, 200, 12, frac(ship.shields(), ship.maxShields()),
                Palette.SHIELD, Palette.GRID);
        UiKit.text(g, ship.shields() + "/" + ship.maxShields(), tx + 282, ty, UiKit.BODY, Palette.INK);
        ty += 24;
        UiKit.text(g, "Crew: " + (ship.crew() == null ? 0 : ship.crew().size()),
                tx, ty, UiKit.BODY, Palette.INK_DIM);
        ty += 30;

        int bw = lw - 36, bh = 38;
        buttons.add(new Btn(tx, ty, bw, bh, "Repair all", true, new Runnable() {
            public void run() { note = msg(c.repairAll()); }
        }));
        ty += bh + 10;
        buttons.add(new Btn(tx, ty, bw, bh, "Recruit crew", true, new Runnable() {
            public void run() { note = msg(c.recruitCrew()); }
        }));
        ty += bh + 10;
        // "Bigger hull" — no dedicated intent exists; the HULL tech track raises max hull.
        buttons.add(new Btn(tx, ty, bw, bh, "Upgrade hull (HULL tech)", true, new Runnable() {
            public void run() { note = msg(c.buyTech(Enums.TechType.HULL)); }
        }));

        // ---- right: tech tree ----
        int rx = lx + lw + 20, ry = 96, rw = w - rx - 24;
        UiKit.panel(g, rx, ry, rw, h - ry - 30);
        UiKit.text(g, "TECH & UPGRADES", rx + 18, ry + 30, UiKit.H2, Palette.INK);

        List<Views.TechView> techs = v.techTree();
        int rowY = ry + 52;
        int rowH = 46;
        int credits = v.credits();
        if (techs == null || techs.isEmpty()) {
            UiKit.text(g, "no tech available", rx + 18, rowY + 10, UiKit.BODY, Palette.INK_DIM);
        } else {
            for (int i = 0; i < techs.size(); i++) {
                final Views.TechView t = techs.get(i);
                int y = rowY + i * rowH;
                UiKit.text(g, title(t.type().name()), rx + 18, y + 16, UiKit.BODY, Palette.INK);
                UiKit.text(g, "Lv " + t.level() + "/" + t.maxLevel(),
                        rx + 150, y + 16, UiKit.BODY, Palette.INK_DIM);
                // progress pips
                for (int p = 0; p < t.maxLevel(); p++) {
                    g.setColor(p < t.level() ? Palette.ACCENT : Palette.GRID);
                    g.fillRect(rx + 230 + p * 10, y + 6, 7, 12);
                }
                boolean canBuy = !t.maxed() && credits >= t.cost();
                String label = t.maxed() ? "MAX" : "Buy " + t.cost();
                Btn b = new Btn(rx + rw - 130, y - 2, 112, 30, label, canBuy, new Runnable() {
                    public void run() { note = msg(c.buyTech(t.type())); }
                });
                buttons.add(b);
            }
        }

        for (Btn b : buttons) b.draw(g, mx, my);
        if (!note.isEmpty())
            UiKit.text(g, note, 24, h - 12, UiKit.BODY, Palette.ACCENT_WARM);
    }

    @Override public void mouseMoved(MouseEvent e) { mx = e.getX(); my = e.getY(); }

    @Override public void mousePressed(MouseEvent e) {
        mx = e.getX(); my = e.getY();
        for (int i = buttons.size() - 1; i >= 0; i--) {
            Btn b = buttons.get(i);
            if (b.hit(mx, my)) { b.action.run(); return; }
        }
    }

    private static double frac(int a, int b) { return b <= 0 ? 0 : (double) a / b; }

    private static String msg(ActionResult r) {
        if (r == null) return "";
        return r.message() == null ? "" : r.message();
    }

    private static String title(String enumName) {
        String s = enumName.toLowerCase().replace('_', ' ');
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
