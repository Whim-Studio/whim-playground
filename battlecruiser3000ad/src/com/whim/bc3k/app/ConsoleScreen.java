package com.whim.bc3k.app;

import com.whim.bc3k.api.Enums;
import com.whim.bc3k.api.GameController;
import com.whim.bc3k.api.Screen;
import com.whim.bc3k.api.Views;
import com.whim.bc3k.render.Palette;
import com.whim.bc3k.render.Starfield;
import com.whim.bc3k.render.UiKit;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * One bridge console. Shared chrome (always-on HUD + function-key tab bar) wraps a
 * per-console body. NAV, TACTICAL, ENGINEERING, POWER and PERSONNEL are live over
 * the {@link GameController} seam; the rest show labelled placeholders.
 */
public final class ConsoleScreen implements Screen {
    private final GameController c;
    private final Enums.Mode consoleMode;
    private final Starfield stars = new Starfield(120, 11L);
    private int cursor = 0;   // selection index (POWER system / ENG system / crew row)

    public ConsoleScreen(GameController c, Enums.Mode consoleMode) {
        this.c = c;
        this.consoleMode = consoleMode;
    }

    @Override public Enums.Mode mode() { return consoleMode; }
    @Override public void onEnter() {}
    @Override public void onExit() {}
    @Override public void update(double dt) { stars.update(dt); }

    @Override public void render(Graphics2D g, int w, int h) {
        UiKit.antialias(g);
        stars.render(g, w, h);
        Views.GameView v = c.view();
        drawHud(g, w, v);
        drawBody(g, w, h, v);
        drawTabBar(g, w, h);
        drawFlash(g, w, v);
        if (v.paused()) UiKit.textCenter(g, "— PAUSED —", w / 2, h / 2, UiKit.H1, Palette.ACCENT_WARM);
    }

    /** Transient mission banner just under the HUD. */
    private void drawFlash(Graphics2D g, int w, Views.GameView v) {
        String f = v.flash();
        if (f == null || f.isEmpty()) return;
        int tw = g.getFontMetrics(UiKit.BODY).stringWidth(f) + 28;
        int x = (w - tw) / 2, y = 92;
        g.setColor(Palette.BG_PANEL);
        g.fillRoundRect(x, y, tw, 22, 8, 8);
        g.setColor(Palette.ACCENT);
        g.drawRoundRect(x, y, tw, 22, 8, 8);
        UiKit.textCenter(g, f, w / 2, y + 16, UiKit.BODY, Palette.ACCENT);
    }

    // ---- top HUD ----
    private void drawHud(Graphics2D g, int w, Views.GameView v) {
        Views.ShipView s = v.ship();
        UiKit.panel(g, 12, 12, w - 24, 74);
        UiKit.text(g, s.name() + "  —  " + v.gameMode().title(), 28, 38, UiKit.H2, Palette.INK);
        g.setColor(Palette.alert(s.alert()));
        g.fillRoundRect(w - 150, 22, 122, 22, 8, 8);
        UiKit.textCenter(g, "ALERT " + s.alert().name(), w - 89, 38, UiKit.BODY, Palette.BG);
        UiKit.text(g, "HULL", 28, 70, UiKit.BODY, Palette.INK_DIM);
        UiKit.bar(g, 78, 58, 200, 14, s.hull() / (double) s.maxHull(), Palette.HULL, Palette.GRID);
        UiKit.text(g, "SHIELD", 300, 70, UiKit.BODY, Palette.INK_DIM);
        UiKit.bar(g, 360, 58, 200, 14, s.shields() / (double) s.maxShields(), Palette.SHIELD, Palette.GRID);
        String reactor = s.reactorOnline()
                ? ("REACTOR " + s.reactorUsed() + "/" + s.reactorOutput())
                : "REACTOR OFFLINE (Shift+R)";
        UiKit.text(g, reactor, 590, 70, UiKit.BODY, s.reactorOnline() ? Palette.GOOD : Palette.BAD);
    }

    // ---- centre body ----
    private void drawBody(Graphics2D g, int w, int h, Views.GameView v) {
        int x = 12, y = 100, bw = w - 24, bh = h - 100 - 64;
        UiKit.panel(g, x, y, bw, bh);
        g.setColor(Palette.console(consoleMode));
        g.fillRect(x + 1, y + 1, bw - 2, 6);
        UiKit.text(g, title(consoleMode), x + 20, y + 40, UiKit.H1, Palette.INK);

        int ix = x + 20, iy = y + 70;
        switch (consoleMode) {
            case NAV:         drawNav(g, ix, iy, bw - 40, bh - 90, v); break;
            case POWER:       drawPower(g, ix, iy, v); break;
            case ENGINEERING: drawEngineering(g, ix, iy, v); break;
            case PERSONNEL:   drawPersonnel(g, ix, iy, v); break;
            case TACTICAL:    drawTactical(g, ix, iy, v); break;
            case COMMS:       drawComms(g, ix, iy, v); break;
            case CARGO:       drawCargo(g, ix, iy, v); break;
            case FLIGHTDECK:  drawFlightDeck(g, ix, iy, v); break;
            default: break;
        }

        // event log tail
        List<String> log = v.log();
        int ly = y + bh - 18;
        for (int i = log.size() - 1; i >= 0 && i >= log.size() - 3; i--) {
            UiKit.text(g, "> " + log.get(i), x + 20, ly, UiKit.MONO, Palette.INK_DIM);
            ly -= 18;
        }
    }

    // ---- NAV: star map + reachable list ----
    private void drawNav(Graphics2D g, int x, int y, int w, int h, Views.GameView v) {
        Views.GalaxyView gal = v.galaxy();
        List<Views.SystemView> sys = gal.systems();
        // Confine the star map to its panel so nodes/labels can't overflow the chrome.
        java.awt.Shape oldClip = g.getClip();
        g.clipRect(x - 4, y - 34, w + 8, h + 8);
        // links
        g.setStroke(new BasicStroke(1.2f));
        for (Views.SystemView a : sys) {
            for (Integer bId : a.links()) {
                if (bId <= a.id()) continue;
                Views.SystemView b = find(sys, bId);
                if (b == null) continue;
                g.setColor(Palette.GRID);
                g.drawLine(x + a.x(), y + a.y() - 40, x + b.x(), y + b.y() - 40);
            }
        }
        // nodes
        for (Views.SystemView n : sys) {
            int px = x + n.x(), py = y + n.y() - 40;
            if (n.current()) { g.setColor(Palette.ACCENT); g.fillOval(px - 9, py - 9, 18, 18); }
            else if (n.reachable()) { g.setColor(Palette.ACCENT_WARM); g.fillOval(px - 6, py - 6, 12, 12); }
            else { g.setColor(n.visited() ? Palette.INK : Palette.INK_DIM); g.fillOval(px - 5, py - 5, 10, 10); }
            if (n.hasStation()) { g.setColor(Palette.GOOD); g.drawOval(px - 12, py - 12, 24, 24); }
            UiKit.text(g, n.name(), px + 12, py + 4, UiKit.BODY, n.current() ? Palette.ACCENT : Palette.INK_DIM);
        }
        // reachable list with jump keys
        List<Views.SystemView> reach = reachable(v);
        int ry = y + 6;
        UiKit.text(g, "JUMP TO:", x, ry, UiKit.H2, Palette.INK);
        ry += 26;
        if (reach.isEmpty()) UiKit.text(g, "(no links)", x, ry, UiKit.BODY, Palette.INK_DIM);
        for (int i = 0; i < reach.size(); i++) {
            UiKit.text(g, "[" + (i + 1) + "] " + reach.get(i).name(), x, ry, UiKit.BODY, Palette.INK);
            ry += 22;
        }
        g.setClip(oldClip);
    }

    // ---- POWER (live) ----
    private void drawPower(Graphics2D g, int x, int y, Views.GameView v) {
        Views.ShipView s = v.ship();
        UiKit.text(g, "Up/Down select - Left/Right adjust (cap " + s.maxPerSystem()
                + "/system, " + s.reactorOutput() + " total)", x, y, UiKit.BODY, Palette.INK_DIM);
        Enums.PowerSystem[] all = Enums.PowerSystem.values();
        int ry = y + 30;
        for (int i = 0; i < all.length; i++) {
            boolean sel = i == cursor % all.length;
            UiKit.text(g, (sel ? "> " : "  ") + all[i].label(), x, ry + 12, UiKit.BODY, sel ? Palette.ACCENT : Palette.INK);
            UiKit.bar(g, x + 160, ry, 260, 14, s.power(all[i]) / (double) s.maxPerSystem(), Palette.ACCENT, Palette.GRID);
            UiKit.text(g, String.valueOf(s.power(all[i])), x + 434, ry + 12, UiKit.MONO, Palette.INK);
            ry += 30;
        }
    }

    // ---- ENGINEERING: integrity + repair ----
    private void drawEngineering(Graphics2D g, int x, int y, Views.GameView v) {
        Views.ShipView s = v.ship();
        UiKit.text(g, "Up/Down select - R repair selected subsystem", x, y, UiKit.BODY, Palette.INK_DIM);
        Enums.PowerSystem[] all = Enums.PowerSystem.values();
        int ry = y + 30;
        for (int i = 0; i < all.length; i++) {
            boolean sel = i == cursor % all.length;
            boolean breach = s.breached(all[i]);
            UiKit.text(g, (sel ? "> " : "  ") + all[i].label(), x, ry + 12, UiKit.BODY, sel ? Palette.ACCENT : Palette.INK);
            UiKit.bar(g, x + 160, ry, 260, 14, s.integrity(all[i]) / 100.0,
                    breach ? Palette.BAD : Palette.GOOD, Palette.GRID);
            UiKit.text(g, s.integrity(all[i]) + "%" + (breach ? "  BREACH" : ""),
                    x + 434, ry + 12, UiKit.MONO, breach ? Palette.BAD : Palette.INK);
            ry += 30;
        }
    }

    // ---- PERSONNEL: roster ----
    private void drawPersonnel(Graphics2D g, int x, int y, Views.GameView v) {
        List<Views.CrewView> crew = v.crew();
        UiKit.text(g, "Up/Down select - G galley - Q quarters - C clone from DNA",
                x, y, UiKit.BODY, Palette.INK_DIM);
        UiKit.text(g, "NAME              HP   HUNGER  FATIGUE  LOCATION", x, y + 28, UiKit.MONO, Palette.INK_DIM);
        int ry = y + 50;
        for (int i = 0; i < crew.size(); i++) {
            Views.CrewView m = crew.get(i);
            boolean sel = i == cursor % Math.max(1, crew.size());
            String row = String.format("%-16s %3d   %3d     %3d      %s",
                    trim(m.name(), 16), m.health(), m.hunger(), m.fatigue(), m.location());
            UiKit.text(g, (sel ? "> " : "  ") + row, x, ry, UiKit.MONO,
                    !m.alive() ? Palette.BAD : (sel ? Palette.ACCENT : Palette.INK));
            ry += 22;
        }
    }

    // ---- TACTICAL: combat readout ----
    private void drawTactical(Graphics2D g, int x, int y, Views.GameView v) {
        Views.CombatView cb = v.combat();
        if (cb == null) {
            UiKit.text(g, "No contacts. (Free Flight — hostiles disabled.)", x, y + 4, UiKit.BODY, Palette.INK_DIM);
            return;
        }
        UiKit.text(g, "TARGET: " + cb.enemyName(), x, y + 4, UiKit.H2, Palette.BAD);
        UiKit.text(g, "HULL", x, y + 38, UiKit.BODY, Palette.INK_DIM);
        UiKit.bar(g, x + 60, y + 26, 300, 16, cb.enemyHull() / (double) cb.enemyMaxHull(), Palette.HULL, Palette.GRID);
        UiKit.text(g, "SHIELD", x, y + 66, UiKit.BODY, Palette.INK_DIM);
        UiKit.bar(g, x + 60, y + 54, 300, 16, cb.enemyShields() / (double) Math.max(1, cb.enemyMaxShields()), Palette.SHIELD, Palette.GRID);
        UiKit.text(g, "FIGHTERS  you " + cb.playerFighters() + "  vs  enemy wing " + cb.enemyFighters()
                + "   (launch fighters on FLIGHT DECK / F8)", x, y + 92, UiKit.BODY, Palette.INK_DIM);
        if (cb.over()) {
            UiKit.text(g, cb.playerWon() ? "ENEMY DESTROYED — press F1..F8 to stand down"
                    : "BATTLECRUISER LOST", x, y + 110, UiKit.H2, cb.playerWon() ? Palette.GOOD : Palette.BAD);
        } else {
            UiKit.text(g, "SPACE: fire weapons  (boost WEAPONS on the PWR console for more damage)",
                    x, y + 110, UiKit.BODY, Palette.ACCENT_WARM);
        }
    }

    // ---- COMMS: traffic log + hailing + campaign objectives ----
    private void drawComms(Graphics2D g, int x, int y, Views.GameView v) {
        Views.CampaignView cam = v.campaign();
        int ry;
        if (cam != null) {
            UiKit.text(g, "H: hail   -   Enter: mark objective complete   -   Ctrl+S: tow",
                    x, y, UiKit.BODY, Palette.INK_DIM);
            UiKit.text(g, "GAMMULAN THREAT", x, y + 30, UiKit.BODY, Palette.INK_DIM);
            UiKit.bar(g, x + 160, y + 18, 240, 14, cam.threat() / 100.0,
                    cam.critical() ? Palette.BAD : Palette.ACCENT_WARM, Palette.GRID);
            UiKit.text(g, cam.threat() + "%" + (cam.critical() ? "  CRITICAL" : ""),
                    x + 412, y + 30, UiKit.MONO, cam.critical() ? Palette.BAD : Palette.INK);
            UiKit.text(g, "OBJECTIVE (" + cam.resolved() + " done): " + cam.objective(),
                    x, y + 58, UiKit.BODY, Palette.ACCENT_WARM);
            ry = y + 90;
        } else {
            UiKit.text(g, "H: hail nearest station   -   Ctrl+S: request tow", x, y, UiKit.BODY, Palette.INK_DIM);
            String brief = v.combat() != null ? "Tactical net: hostile contact — weapons free."
                                              : "GALCOM net quiet. Standard patrol conditions.";
            UiKit.text(g, brief, x, y + 26, UiKit.BODY, Palette.ACCENT_WARM);
            ry = y + 58;
        }
        UiKit.text(g, "— CHANNEL TRAFFIC —", x, ry, UiKit.H2, Palette.INK);
        ry += 26;
        List<String> log = v.log();
        for (int i = log.size() - 1; i >= 0 && ry < y + 320; i--) {
            UiKit.text(g, log.get(i), x, ry, UiKit.MONO, Palette.INK_DIM);
            ry += 20;
        }
    }

    // ---- CARGO: logistics manifest ----
    private void drawCargo(Graphics2D g, int x, int y, Views.GameView v) {
        Views.CargoView c0 = v.cargo();
        UiKit.text(g, "R: refuel at starstation", x, y, UiKit.BODY, Palette.INK_DIM);
        int ry = y + 34;
        UiKit.text(g, "CREDITS", x, ry + 12, UiKit.BODY, Palette.INK_DIM);
        UiKit.text(g, String.valueOf(c0.credits()), x + 140, ry + 12, UiKit.MONO, Palette.INK); ry += 30;
        UiKit.text(g, "FUEL", x, ry + 12, UiKit.BODY, Palette.INK_DIM);
        UiKit.bar(g, x + 140, ry, 240, 14, c0.fuel() / (double) c0.maxFuel(), Palette.ACCENT_WARM, Palette.GRID);
        UiKit.text(g, c0.fuel() + "/" + c0.maxFuel(), x + 392, ry + 12, UiKit.MONO, Palette.INK); ry += 30;
        UiKit.text(g, "SPARE PARTS", x, ry + 12, UiKit.BODY, Palette.INK_DIM);
        UiKit.text(g, String.valueOf(c0.spareParts()), x + 140, ry + 12, UiKit.MONO, Palette.INK); ry += 30;
        UiKit.text(g, "ORDNANCE", x, ry + 12, UiKit.BODY, Palette.INK_DIM);
        UiKit.text(g, String.valueOf(c0.ordnance()), x + 140, ry + 12, UiKit.MONO, Palette.INK);
    }

    // ---- FLIGHT DECK: craft complement ----
    private void drawFlightDeck(Graphics2D g, int x, int y, Views.GameView v) {
        List<Views.CraftView> craft = v.craft();
        UiKit.text(g, "Up/Down select - L launch - R recall - D deploy ATVs to surface",
                x, y, UiKit.BODY, Palette.INK_DIM);
        UiKit.text(g, "CRAFT        DOCKED   OUT   TOTAL", x, y + 28, UiKit.MONO, Palette.INK_DIM);
        int ry = y + 50;
        for (int i = 0; i < craft.size(); i++) {
            Views.CraftView cr = craft.get(i);
            boolean sel = i == cursor % Math.max(1, craft.size());
            String row = String.format("%-11s %4d   %4d   %4d",
                    cr.type().name(), cr.docked(), cr.launched(), cr.total());
            UiKit.text(g, (sel ? "> " : "  ") + row, x, ry, UiKit.MONO, sel ? Palette.ACCENT : Palette.INK);
            ry += 24;
        }
    }

    // ---- bottom tab bar ----
    private void drawTabBar(Graphics2D g, int w, int h) {
        int n = ScreenManager.CONSOLES.length, pad = 12, gap = 6;
        int tw = (w - pad * 2 - gap * (n - 1)) / n, y = h - 52, th = 38;
        for (int i = 0; i < n; i++) {
            Enums.Mode m = ScreenManager.CONSOLES[i];
            int tx = pad + i * (tw + gap);
            boolean active = m == consoleMode;
            g.setColor(active ? Palette.console(m) : Palette.BG_PANEL);
            g.fillRoundRect(tx, y, tw, th, 8, 8);
            g.setColor(Palette.GRID); g.drawRoundRect(tx, y, tw, th, 8, 8);
            UiKit.textCenter(g, "F" + (i + 1), tx + tw / 2, y + 15, UiKit.BODY, active ? Palette.INK : Palette.INK_DIM);
            UiKit.textCenter(g, shortName(m), tx + tw / 2, y + 30, UiKit.BODY, active ? Palette.INK : Palette.INK_DIM);
        }
    }

    // ---- input ----
    @Override public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        switch (consoleMode) {
            case POWER: {
                Enums.PowerSystem[] all = Enums.PowerSystem.values();
                if (code == KeyEvent.VK_UP) cursor = (cursor - 1 + all.length) % all.length;
                else if (code == KeyEvent.VK_DOWN) cursor = (cursor + 1) % all.length;
                else if (code == KeyEvent.VK_LEFT) c.setPower(all[cursor % all.length], -5);
                else if (code == KeyEvent.VK_RIGHT) c.setPower(all[cursor % all.length], +5);
                break;
            }
            case ENGINEERING: {
                Enums.PowerSystem[] all = Enums.PowerSystem.values();
                if (code == KeyEvent.VK_UP) cursor = (cursor - 1 + all.length) % all.length;
                else if (code == KeyEvent.VK_DOWN) cursor = (cursor + 1) % all.length;
                else if (code == KeyEvent.VK_R) c.repair(all[cursor % all.length]);
                break;
            }
            case NAV: {
                List<Views.SystemView> reach = reachable(c.view());
                if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                    int idx = code - KeyEvent.VK_1;
                    if (idx < reach.size()) c.jumpTo(reach.get(idx).id());
                }
                break;
            }
            case PERSONNEL: {
                List<Views.CrewView> crew = c.view().crew();
                int n = Math.max(1, crew.size());
                if (code == KeyEvent.VK_UP) cursor = (cursor - 1 + n) % n;
                else if (code == KeyEvent.VK_DOWN) cursor = (cursor + 1) % n;
                else if (code == KeyEvent.VK_G && !crew.isEmpty()) c.orderCrew(crew.get(cursor % n).id(), Enums.CrewOrder.EAT);
                else if (code == KeyEvent.VK_Q && !crew.isEmpty()) c.orderCrew(crew.get(cursor % n).id(), Enums.CrewOrder.REST);
                else if (code == KeyEvent.VK_C) c.cloneCrew();
                break;
            }
            case TACTICAL:
                if (code == KeyEvent.VK_SPACE) c.fireWeapons();
                break;
            case COMMS:
                if (code == KeyEvent.VK_H) c.hail();
                else if (code == KeyEvent.VK_ENTER) c.resolveObjective();
                break;
            case CARGO:
                if (code == KeyEvent.VK_R) c.refuel();
                break;
            case FLIGHTDECK: {
                Enums.CraftType[] all = Enums.CraftType.values();
                if (code == KeyEvent.VK_UP) cursor = (cursor - 1 + all.length) % all.length;
                else if (code == KeyEvent.VK_DOWN) cursor = (cursor + 1) % all.length;
                else if (code == KeyEvent.VK_L) c.launchCraft(all[cursor % all.length]);
                else if (code == KeyEvent.VK_R) c.recallCraft(all[cursor % all.length]);
                else if (code == KeyEvent.VK_D) c.deployAtv();
                break;
            }
            default: break;
        }
    }

    // ---- helpers ----
    private static List<Views.SystemView> reachable(Views.GameView v) {
        List<Views.SystemView> out = new ArrayList<Views.SystemView>();
        for (Views.SystemView s : v.galaxy().systems()) if (s.reachable()) out.add(s);
        return out;
    }
    private static Views.SystemView find(List<Views.SystemView> list, int id) {
        for (Views.SystemView s : list) if (s.id() == id) return s;
        return null;
    }
    private static String trim(String s, int n) { return s.length() <= n ? s : s.substring(0, n); }

    private static String title(Enums.Mode m) {
        switch (m) {
            case NAV: return "NAVIGATION";
            case TACTICAL: return "TACTICAL / WEAPONS";
            case ENGINEERING: return "ENGINEERING";
            case POWER: return "POWER ALLOCATION";
            case COMMS: return "COMMUNICATIONS";
            case CARGO: return "CARGO / LOGISTICS";
            case PERSONNEL: return "PERSONNEL";
            case FLIGHTDECK: return "FLIGHT DECK";
            default: return m.name();
        }
    }
    private static String shortName(Enums.Mode m) {
        switch (m) {
            case NAV: return "NAV";
            case TACTICAL: return "TAC";
            case ENGINEERING: return "ENG";
            case POWER: return "PWR";
            case COMMS: return "COMM";
            case CARGO: return "CARGO";
            case PERSONNEL: return "CREW";
            case FLIGHTDECK: return "DECK";
            default: return m.name();
        }
    }
}
