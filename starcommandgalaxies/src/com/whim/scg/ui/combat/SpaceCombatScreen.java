package com.whim.scg.ui.combat;

import com.whim.scg.api.ActionResult;
import com.whim.scg.api.Enums;
import com.whim.scg.api.GameController;
import com.whim.scg.api.GridPos;
import com.whim.scg.api.Screen;
import com.whim.scg.api.Vec2;
import com.whim.scg.api.Views;
import com.whim.scg.render.Palette;
import com.whim.scg.render.Starfield;
import com.whim.scg.render.UiKit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SPACE_COMBAT — the marquee screen. Real-time-with-pause duel. Draws both ships
 * as room schematics with hull/shield bars, fires and breaches, live projectiles,
 * and a per-weapon control panel (charge bar, power +/-, target, fire). Click an
 * enemy room to target the selected weapon at it. Space bar (handled by the shell)
 * toggles pause; orders may still be issued while paused. Begin-boarding when the
 * enemy shields are down, or flee. Null-safe against the stub controller.
 */
public final class SpaceCombatScreen implements Screen {

    private final GameController c;
    private final Starfield stars = new Starfield(220, 91L);

    private int mx, my;
    private int selWeapon = 0;                 // active weapon slot for targeting
    private String note = "";
    private final List<Btn> buttons = new ArrayList<Btn>();
    private final Map<Integer, Rectangle> enemyRoomRects = new HashMap<Integer, Rectangle>();

    public SpaceCombatScreen(GameController c) { this.c = c; }

    @Override public Enums.Mode mode() { return Enums.Mode.SPACE_COMBAT; }
    @Override public void onEnter() { note = ""; selWeapon = 0; }
    @Override public void onExit() {}
    @Override public void update(double dt) { stars.update(dt); }

    // ------------------------------------------------------------------ render
    @Override public void render(Graphics2D g, int w, int h) {
        UiKit.antialias(g);
        stars.render(g, w, h);
        buttons.clear();
        enemyRoomRects.clear();

        Views.GameView v = c.view();
        Views.CombatView combat = v == null ? null : v.combat();

        UiKit.text(g, "SPACE COMBAT", 24, 40, UiKit.H2, Palette.ACCENT);
        if (v != null)
            UiKit.text(g, "Credits " + v.credits() + "   ·   Day " + v.day(),
                    220, 40, UiKit.BODY, Palette.INK_DIM);

        if (combat == null) {
            UiKit.panel(g, 24, 60, w - 48, h - 160);
            UiKit.textCenter(g, "AWAITING ENGINE", w / 2, h / 2 - 12, UiKit.H2, Palette.INK_DIM);
            UiKit.textCenter(g, "no combat in progress — no CombatView data",
                    w / 2, h / 2 + 14, UiKit.BODY, Palette.INK_DIM);
            drawGlobalButtons(g, v, combat, w, h);
            for (Btn b : buttons) b.draw(g, mx, my);
            return;
        }

        Views.ShipView player = combat.player() != null ? combat.player() : v.playerShip();
        Views.ShipView enemy = combat.enemy();

        int arenaTop = 64;
        int arenaH = h - 260;
        int halfW = (w - 60) / 2;

        // player ship (left), enemy ship (right)
        if (player != null) drawShip(g, player, 24, arenaTop, halfW, arenaH, false, -1);
        int enemyTargetHi = selectedWeaponTarget(player);
        if (enemy != null) drawShip(g, enemy, 36 + halfW, arenaTop, halfW, arenaH, true, enemyTargetHi);

        drawProjectiles(g, combat, 24, arenaTop, w - 48, arenaH);

        drawWeaponsPanel(g, v, player, w, h);
        drawGlobalButtons(g, v, combat, w, h);
        drawLog(g, v, w, h);

        for (Btn b : buttons) b.draw(g, mx, my);

        if (v.paused()) drawPausedBanner(g, w);
        if (combat.over()) drawOverlay(g, w, h, combat.playerWon());
        if (!note.isEmpty())
            UiKit.text(g, note, 24, h - 8, UiKit.BODY, Palette.ACCENT_WARM);
    }

    // -------------------------------------------------------------- ship draw
    private void drawShip(Graphics2D g, Views.ShipView s, int x, int y, int w, int h,
                          boolean isEnemy, int highlightRoomId) {
        // header: name + hull/shield bars
        UiKit.text(g, s.name() + (isEnemy ? "  (hostile)" : "  (you)"),
                x + 6, y + 4, UiKit.H2, isEnemy ? Palette.BAD : Palette.ACCENT);
        int barY = y + 14;
        UiKit.text(g, "HULL", x + 6, barY + 12, UiKit.BODY, Palette.INK_DIM);
        UiKit.bar(g, x + 54, barY + 1, w - 150, 12, frac(s.hull(), s.maxHull()),
                Palette.HULL, Palette.GRID);
        UiKit.text(g, s.hull() + "/" + s.maxHull(), x + w - 90, barY + 12, UiKit.BODY, Palette.INK);
        barY += 18;
        UiKit.text(g, "SHLD", x + 6, barY + 12, UiKit.BODY, Palette.INK_DIM);
        UiKit.bar(g, x + 54, barY + 1, w - 150, 12, frac(s.shields(), s.maxShields()),
                Palette.SHIELD, Palette.GRID);
        UiKit.text(g, s.shields() + "/" + s.maxShields(), x + w - 90, barY + 12, UiKit.BODY, Palette.INK);

        // room grid
        int gridTop = y + 58;
        int gridH = h - 62;
        List<Views.RoomView> rooms = s.rooms();
        int gw = Math.max(1, s.gridW());
        int gh = Math.max(1, s.gridH());
        int cell = Math.min((w - 12) / gw, gridH / gh);
        if (cell < 6) cell = 6;
        int gx = x + (w - cell * gw) / 2;
        int gy = gridTop;

        // faint grid backdrop
        g.setColor(Palette.GRID);
        g.setStroke(new BasicStroke(1f));
        g.drawRect(gx, gy, cell * gw, cell * gh);

        if (rooms != null) {
            for (Views.RoomView r : rooms) {
                GridPos o = r.origin();
                if (o == null) continue;
                int rx = gx + o.x * cell, ry = gy + o.y * cell;
                int rw = Math.max(1, r.w()) * cell, rh = Math.max(1, r.h()) * cell;
                g.setColor(Palette.room(r.type()));
                g.fillRect(rx + 1, ry + 1, rw - 2, rh - 2);

                // integrity shading (darken as hp drops)
                double dmg = 1.0 - frac(r.hp(), r.maxHp());
                if (dmg > 0) {
                    g.setColor(new Color(0, 0, 0, (int) (Math.min(0.7, dmg) * 255)));
                    g.fillRect(rx + 1, ry + 1, rw - 2, rh - 2);
                }
                // breach
                if (r.breached()) {
                    g.setColor(Palette.BREACH);
                    g.fillRect(rx + 1, ry + 1, rw - 2, rh - 2);
                    g.setColor(Palette.SHIELD);
                    UiKit.textCenter(g, "⤡", rx + rw / 2, ry + rh / 2 + 5, UiKit.BODY, Palette.SHIELD);
                }
                // fire
                if (r.onFire()) {
                    g.setColor(Palette.FIRE);
                    UiKit.textCenter(g, "🔥", rx + rw / 2, ry + rh / 2 + 5, UiKit.BODY, Palette.FIRE);
                }
                // target highlight (enemy)
                if (isEnemy && r.id() == highlightRoomId) {
                    g.setColor(Palette.ACCENT_WARM);
                    g.setStroke(new BasicStroke(2.4f));
                    g.drawRect(rx + 1, ry + 1, rw - 3, rh - 3);
                }
                // outline + crew count
                g.setColor(Palette.GRID);
                g.setStroke(new BasicStroke(1f));
                g.drawRect(rx, ry, rw, rh);
                if (r.crewIds() != null && !r.crewIds().isEmpty()) {
                    g.setColor(Palette.INK);
                    g.fillOval(rx + rw - 8, ry + 3, 5, 5);
                }
                // short type tag
                if (cell >= 18) {
                    UiKit.textCenter(g, tag(r.type()), rx + rw / 2, ry + rh / 2 - 2,
                            UiKit.BODY, new Color(230, 236, 245, 150));
                }
                if (isEnemy) enemyRoomRects.put(r.id(), new Rectangle(rx, ry, rw, rh));
            }
        }
    }

    private void drawProjectiles(Graphics2D g, Views.CombatView combat,
                                 int ax, int ay, int aw, int ah) {
        List<Views.ProjectileView> ps = combat.projectiles();
        if (ps == null) return;
        for (Views.ProjectileView p : ps) {
            Vec2 pos = p.pos();
            if (pos == null) continue;
            // Coordinate space is engine-defined; heuristically treat <=1.5 as
            // normalized, otherwise as raw pixels. Clamp to the arena either way.
            double nx, ny;
            if (Math.abs(pos.x) <= 1.5 && Math.abs(pos.y) <= 1.5) {
                nx = ax + pos.x * aw; ny = ay + pos.y * ah;
            } else {
                nx = pos.x; ny = pos.y;
            }
            int px = (int) Math.max(ax, Math.min(ax + aw, nx));
            int py = (int) Math.max(ay, Math.min(ay + ah, ny));
            Color col = weaponColor(p.type());
            g.setColor(col);
            g.fillOval(px - 3, py - 3, 6, 6);
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 90));
            int dir = p.fromPlayer() ? -6 : 6;
            g.fillOval(px - 3 + dir, py - 2, 4, 4);
        }
    }

    // ---------------------------------------------------------- weapons panel
    private void drawWeaponsPanel(Graphics2D g, Views.GameView v, Views.ShipView player,
                                  int w, int h) {
        int panelY = h - 190;
        int panelH = 150;
        UiKit.panel(g, 24, panelY, w - 260, panelH);
        UiKit.text(g, "WEAPONS", 40, panelY + 24, UiKit.H2, Palette.INK);
        UiKit.text(g, "select a weapon, then click an enemy room to target",
                150, panelY + 24, UiKit.BODY, Palette.INK_DIM);

        List<Views.WeaponView> weapons = player == null ? null : player.weapons();
        if (weapons == null || weapons.isEmpty()) {
            UiKit.text(g, "no weapons online", 40, panelY + 60, UiKit.BODY, Palette.INK_DIM);
            return;
        }
        if (selWeapon >= weapons.size()) selWeapon = 0;

        int rowY = panelY + 40;
        int rowH = (panelH - 48) / Math.max(1, weapons.size());
        if (rowH > 34) rowH = 34;
        int rowW = w - 260 - 32;

        for (int i = 0; i < weapons.size(); i++) {
            final Views.WeaponView wv = weapons.get(i);
            final int slot = wv.slot();
            int y = rowY + i * rowH;
            boolean sel = i == selWeapon;

            if (sel) {
                g.setColor(new Color(0x39C7C7));
                g.fillRect(34, y - 2, 3, rowH - 4);
            }
            // name + type (clickable to select)
            Color nameCol = sel ? Palette.ACCENT : Palette.INK;
            UiKit.text(g, wv.name(), 44, y + 15, UiKit.BODY, nameCol);
            UiKit.text(g, tagWeapon(wv.type()), 190, y + 15, UiKit.BODY, weaponColor(wv.type()));
            // register a select hotspot spanning the name area
            final int idx = i;
            buttons.add(new Btn(40, y, 220, rowH - 2, "", true, false, new Runnable() {
                public void run() { selWeapon = idx; }
            }));

            // charge bar
            double cf = wv.chargeMax() <= 0 ? (wv.ready() ? 1 : 0) : frac(wv.charge(), wv.chargeMax());
            UiKit.bar(g, 270, y + 4, 150, 10, cf,
                    wv.ready() ? Palette.GOOD : Palette.ACCENT, Palette.GRID);
            UiKit.text(g, wv.ready() ? "READY" : "chg",
                    426, y + 14, UiKit.BODY, wv.ready() ? Palette.GOOD : Palette.INK_DIM);

            // power -/+
            int pxx = 480;
            UiKit.text(g, "PWR " + wv.powered(), pxx, y + 15, UiKit.BODY, Palette.INK_DIM);
            buttons.add(new Btn(pxx + 66, y, 24, rowH - 4, "-", true, false, new Runnable() {
                public void run() { note = msg(c.setWeaponPower(slot, Math.max(0, wv.powered() - 1))); }
            }));
            buttons.add(new Btn(pxx + 94, y, 24, rowH - 4, "+", true, false, new Runnable() {
                public void run() { note = msg(c.setWeaponPower(slot, wv.powered() + 1)); }
            }));

            // target label
            String tgt = wv.targetRoomId() >= 0 ? ("room " + wv.targetRoomId()) : "no target";
            UiKit.text(g, "TGT " + tgt, pxx + 130, y + 15, UiKit.BODY,
                    wv.targetRoomId() >= 0 ? Palette.ACCENT_WARM : Palette.INK_DIM);

            // fire button
            boolean canFire = wv.ready() && wv.targetRoomId() >= 0;
            buttons.add(new Btn(rowW - 66 + 34, y, 60, rowH - 4, "FIRE", canFire, true, new Runnable() {
                public void run() { note = msg(c.fireWeapon(slot)); }
            }));
        }
    }

    private void drawGlobalButtons(Graphics2D g, Views.GameView v, Views.CombatView combat,
                                   int w, int h) {
        int x = w - 220, y = h - 190, bw = 196, bh = 42;
        boolean canBoard = combat != null && combat.canBoard();
        buttons.add(new Btn(x, y, bw, bh, "Begin boarding", canBoard, true, new Runnable() {
            public void run() { note = msg(c.beginBoarding()); }
        }));
        y += bh + 10;
        buttons.add(new Btn(x, y, bw, bh, "Flee combat", combat != null, false, new Runnable() {
            public void run() { note = msg(c.fleeCombat()); }
        }));
        y += bh + 10;
        boolean paused = v != null && v.paused();
        buttons.add(new Btn(x, y, bw, bh, paused ? "Resume (Space)" : "Pause (Space)", true, false,
                new Runnable() { public void run() { c.togglePause(); } }));
    }

    private void drawLog(Graphics2D g, Views.GameView v, int w, int h) {
        if (v == null || v.log() == null) return;
        List<String> log = v.log();
        int lines = Math.min(2, log.size());
        int ly = h - 30;
        for (int i = log.size() - 1; i >= log.size() - lines && i >= 0; i--) {
            UiKit.text(g, log.get(i), 300, ly, UiKit.MONO, Palette.INK_DIM);
            ly -= 16;
        }
    }

    private void drawPausedBanner(Graphics2D g, int w) {
        String s = "PAUSED";
        g.setColor(new Color(0xF0, 0xA9, 0x3B, 60));
        g.fillRect(0, 48, w, 26);
        UiKit.textCenter(g, s + "  —  issue orders freely, press Space to resume",
                w / 2, 67, UiKit.H2, Palette.ACCENT_WARM);
    }

    private void drawOverlay(Graphics2D g, int w, int h, boolean won) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, w, h);
        UiKit.textCenter(g, won ? "ENEMY DESTROYED" : "SHIP LOST",
                w / 2, h / 2 - 6, UiKit.H1, won ? Palette.GOOD : Palette.BAD);
        UiKit.textCenter(g, "combat resolved", w / 2, h / 2 + 26, UiKit.BODY, Palette.INK_DIM);
    }

    // ------------------------------------------------------------------ input
    @Override public void mouseMoved(MouseEvent e) { mx = e.getX(); my = e.getY(); }

    @Override public void mousePressed(MouseEvent e) {
        mx = e.getX(); my = e.getY();
        for (int i = buttons.size() - 1; i >= 0; i--) {
            Btn b = buttons.get(i);
            if (b.hit(mx, my)) { b.action.run(); return; }
        }
        // click enemy room to target the selected weapon
        for (Map.Entry<Integer, Rectangle> en : enemyRoomRects.entrySet()) {
            if (en.getValue().contains(mx, my)) {
                Views.GameView v = c.view();
                Views.ShipView player = v != null && v.combat() != null && v.combat().player() != null
                        ? v.combat().player() : (v != null ? v.playerShip() : null);
                int slot = weaponSlotAt(player, selWeapon);
                if (slot >= 0) note = msg(c.setWeaponTarget(slot, en.getKey()));
                return;
            }
        }
    }

    // ------------------------------------------------------------------ helpers
    private int selectedWeaponTarget(Views.ShipView player) {
        if (player == null || player.weapons() == null) return -1;
        List<Views.WeaponView> ws = player.weapons();
        if (selWeapon < 0 || selWeapon >= ws.size()) return -1;
        return ws.get(selWeapon).targetRoomId();
    }

    private static int weaponSlotAt(Views.ShipView player, int index) {
        if (player == null || player.weapons() == null) return -1;
        List<Views.WeaponView> ws = player.weapons();
        if (index < 0 || index >= ws.size()) return -1;
        return ws.get(index).slot();
    }

    private static double frac(int a, int b) { return b <= 0 ? 0 : Math.max(0, Math.min(1, (double) a / b)); }

    private static Color weaponColor(Enums.WeaponType t) {
        if (t == null) return Palette.INK;
        switch (t) {
            case LASER:         return new Color(0xE0556B);
            case ION:           return new Color(0x5B8DEF);
            case PLASMA_TORPEDO:return new Color(0x8ED04F);
            case MISSILE:       return Palette.ACCENT_WARM;
            case BEAM:          return Palette.ACCENT;
            default:            return Palette.INK;
        }
    }

    private static String tagWeapon(Enums.WeaponType t) {
        if (t == null) return "";
        switch (t) {
            case PLASMA_TORPEDO: return "torpedo";
            default: return t.name().toLowerCase();
        }
    }

    private static String tag(Enums.RoomType t) {
        switch (t) {
            case BRIDGE: return "BRG"; case ENGINES: return "ENG"; case WEAPONS: return "WPN";
            case SHIELDS: return "SHD"; case MEDBAY: return "MED"; case TELEPORTER: return "TEL";
            case OXYGEN: return "O2"; case SENSORS: return "SEN"; case QUARTERS: return "QTR";
            case CARGO: return "CGO"; default: return "";
        }
    }

    private static String msg(ActionResult r) {
        if (r == null) return "";
        return r.message() == null ? "" : r.message();
    }
}
