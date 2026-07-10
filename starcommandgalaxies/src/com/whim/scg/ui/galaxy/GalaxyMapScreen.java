package com.whim.scg.ui.galaxy;

import com.whim.scg.api.ActionResult;
import com.whim.scg.api.Enums;
import com.whim.scg.api.GameController;
import com.whim.scg.api.Screen;
import com.whim.scg.api.Views;
import com.whim.scg.render.Palette;
import com.whim.scg.render.Starfield;
import com.whim.scg.render.UiKit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * GALAXY_MAP — the node-graph star chart. Renders systems + links from
 * {@code view().galaxy()}, lets the player jump to a linked system, scan the
 * current one, dock at a starport, and resolve a pending encounter via a simple
 * popup. Fully null-safe against the stub controller (draws an "awaiting engine"
 * state when {@code galaxy()} is null).
 */
public final class GalaxyMapScreen implements Screen {

    private final GameController c;
    private final Starfield stars = new Starfield(160, 11L);

    private int mx, my;                       // mouse position (viewport px)
    private int selected = -1;                // selected system id (for info panel)
    private boolean eventOpen = false;        // encounter popup visible
    private String note = "";                 // last action message
    private final List<Btn> buttons = new ArrayList<Btn>();

    // cached node screen positions from the last render, for hit testing
    private int[] nodeId = new int[0];
    private int[] nodeX = new int[0];
    private int[] nodeY = new int[0];
    private int nodeCount = 0;

    public GalaxyMapScreen(GameController c) { this.c = c; }

    @Override public Enums.Mode mode() { return Enums.Mode.GALAXY_MAP; }
    @Override public void onEnter() { eventOpen = false; note = ""; }
    @Override public void onExit() {}
    @Override public void update(double dt) { stars.update(dt); }

    // ------------------------------------------------------------------ render
    @Override public void render(Graphics2D g, int w, int h) {
        UiKit.antialias(g);
        stars.render(g, w, h);
        buttons.clear();

        Views.GameView v = c.view();
        Views.GalaxyView gal = v == null ? null : v.galaxy();

        int sideW = 300;
        int mapX = 16, mapY = 56, mapW = w - sideW - 40, mapH = h - mapY - 130;

        drawHeader(g, v, w);

        if (gal == null || gal.systems() == null || gal.systems().isEmpty()) {
            UiKit.panel(g, mapX, mapY, mapW, mapH);
            UiKit.textCenter(g, "AWAITING ENGINE", mapX + mapW / 2, mapY + mapH / 2 - 10,
                    UiKit.H2, Palette.INK_DIM);
            UiKit.textCenter(g, "no galaxy data — start a New Game once the engine is loaded",
                    mapX + mapW / 2, mapY + mapH / 2 + 16, UiKit.BODY, Palette.INK_DIM);
            drawSidebar(g, v, null, sideW, w, h);
            drawLog(g, v, mapX, mapY + mapH + 12, mapW, 96);
            return;
        }

        List<Views.StarSystemView> systems = gal.systems();
        if (selected < 0) selected = gal.currentSystem();

        // map area frame
        g.setColor(Palette.BG_PANEL);
        g.fillRoundRect(mapX, mapY, mapW, mapH, 10, 10);
        g.setColor(Palette.GRID);
        g.drawRoundRect(mapX, mapY, mapW, mapH, 10, 10);

        layoutNodes(systems, gal, mapX + 30, mapY + 30, mapW - 60, mapH - 60);

        Views.StarSystemView current = byId(systems, gal.currentSystem());

        // links (draw once per undirected pair)
        g.setStroke(new BasicStroke(1.4f));
        for (int i = 0; i < systems.size(); i++) {
            Views.StarSystemView s = systems.get(i);
            int ax = screenX(s.id()), ay = screenY(s.id());
            if (s.links() == null) continue;
            for (Integer lk : s.links()) {
                if (lk == null || lk.intValue() <= s.id()) continue; // dedupe
                int bx = screenX(lk.intValue()), by = screenY(lk.intValue());
                if (bx == Integer.MIN_VALUE) continue;
                boolean fromCurrent = current != null &&
                        (s.id() == current.id() || lk.intValue() == current.id());
                g.setColor(fromCurrent ? Palette.ACCENT : Palette.GRID);
                g.drawLine(ax, ay, bx, by);
            }
        }

        // nodes
        boolean[] linkedToCur = new boolean[0];
        for (int i = 0; i < systems.size(); i++) {
            Views.StarSystemView s = systems.get(i);
            int nx = screenX(s.id()), ny = screenY(s.id());
            boolean isCurrent = current != null && s.id() == current.id();
            boolean isLinked = current != null && current.links() != null && current.links().contains(s.id());
            int r = isCurrent ? 13 : 10;

            // link glow ring for jumpable neighbours
            if (isLinked) {
                g.setColor(Palette.ACCENT);
                g.setStroke(new BasicStroke(1.5f));
                g.drawOval(nx - r - 4, ny - r - 4, (r + 4) * 2, (r + 4) * 2);
            }
            Color fill = isCurrent ? Palette.ACCENT
                    : s.visited() ? new Color(0x50607A) : Palette.HULL;
            g.setColor(fill);
            g.fillOval(nx - r, ny - r, r * 2, r * 2);

            // starport marker
            if (s.hasStarport()) {
                g.setColor(Palette.ACCENT_WARM);
                g.fillRect(nx - 3, ny - 3, 6, 6);
            }
            // pending event marker
            Enums.EventType ev = s.pendingEvent();
            if (ev != null && ev != Enums.EventType.NOTHING) {
                g.setColor(Palette.BAD);
                g.fillOval(nx + r - 2, ny - r - 6, 8, 8);
            }
            // selection ring
            if (s.id() == selected) {
                g.setColor(Palette.INK);
                g.setStroke(new BasicStroke(2f));
                g.drawOval(nx - r - 7, ny - r - 7, (r + 7) * 2, (r + 7) * 2);
            }
            // label
            UiKit.textCenter(g, s.name(), nx, ny + r + 15, UiKit.BODY,
                    isCurrent ? Palette.INK : Palette.INK_DIM);
        }

        drawLegend(g, mapX + 12, mapY + mapH - 16);
        drawSidebar(g, v, byId(systems, selected), sideW, w, h);
        drawLog(g, v, mapX, mapY + mapH + 12, mapW, 96);

        if (eventOpen && current != null && current.pendingEvent() != null
                && current.pendingEvent() != Enums.EventType.NOTHING) {
            drawEventPopup(g, w, h, current.pendingEvent());
        }
    }

    private void drawHeader(Graphics2D g, Views.GameView v, int w) {
        UiKit.text(g, "GALAXY CHART", 18, 38, UiKit.H2, Palette.ACCENT);
        if (v != null) {
            String s = "Credits " + v.credits() + "   ·   Day " + v.day();
            UiKit.text(g, s, 210, 38, UiKit.BODY, Palette.INK);
        }
        UiKit.text(g, "click a linked system to jump  ·  arrows to travel  ·  Esc menu",
                w - 470, 38, UiKit.BODY, Palette.INK_DIM);
    }

    private void drawLegend(Graphics2D g, int x, int y) {
        g.setColor(Palette.ACCENT);   g.fillOval(x, y - 8, 9, 9);
        UiKit.text(g, "current", x + 14, y, UiKit.BODY, Palette.INK_DIM);
        g.setColor(Palette.ACCENT_WARM); g.fillRect(x + 84, y - 7, 7, 7);
        UiKit.text(g, "starport", x + 96, y, UiKit.BODY, Palette.INK_DIM);
        g.setColor(Palette.BAD);      g.fillOval(x + 176, y - 8, 8, 8);
        UiKit.text(g, "encounter", x + 190, y, UiKit.BODY, Palette.INK_DIM);
    }

    private void drawSidebar(Graphics2D g, Views.GameView v, Views.StarSystemView sel,
                             int sideW, int w, int h) {
        int x = w - sideW - 12, y = 56, pw = sideW, ph = h - 186;
        UiKit.panel(g, x, y, pw, ph);
        int tx = x + 16, ty = y + 30;
        UiKit.text(g, "SYSTEM", tx, ty, UiKit.H2, Palette.INK); ty += 26;

        if (sel == null) {
            UiKit.text(g, "no system selected", tx, ty, UiKit.BODY, Palette.INK_DIM);
        } else {
            Views.GalaxyView gal = v.galaxy();
            boolean isCurrent = gal != null && sel.id() == gal.currentSystem();
            boolean isLinked = false;
            if (gal != null) {
                Views.StarSystemView cur = byId(gal.systems(), gal.currentSystem());
                isLinked = cur != null && cur.links() != null && cur.links().contains(sel.id());
            }
            UiKit.text(g, sel.name(), tx, ty, UiKit.H2, Palette.ACCENT); ty += 24;
            UiKit.text(g, "status: " + (isCurrent ? "you are here"
                    : sel.visited() ? "visited" : "unexplored"),
                    tx, ty, UiKit.BODY, Palette.INK_DIM); ty += 20;
            UiKit.text(g, "starport: " + (sel.hasStarport() ? "yes" : "no"),
                    tx, ty, UiKit.BODY, Palette.INK_DIM); ty += 20;
            Enums.EventType ev = sel.pendingEvent();
            String evs = (ev == null || ev == Enums.EventType.NOTHING) ? "clear" : title(ev.name());
            UiKit.text(g, "encounter: " + evs, tx, ty, UiKit.BODY,
                    (ev != null && ev != Enums.EventType.NOTHING) ? Palette.BAD : Palette.INK_DIM);
            ty += 30;

            int bw = pw - 32, bx = tx, bh = 34;
            if (isLinked) {
                final int target = sel.id();
                buttons.add(new Btn(bx, ty, bw, bh, "Jump here", true, new Runnable() {
                    public void run() { doJump(target); }
                }));
                ty += bh + 10;
            }
        }

        // current-system actions
        if (v != null && v.galaxy() != null) {
            Views.StarSystemView cur = byId(v.galaxy().systems(), v.galaxy().currentSystem());
            int bw = pw - 32, bx = tx, bh = 34;
            ty = y + ph - 16 - bh * 3 - 20;
            UiKit.text(g, "AT CURRENT SYSTEM", tx, ty - 10, UiKit.BODY, Palette.INK_DIM);
            buttons.add(new Btn(bx, ty, bw, bh, "Scan system", true, new Runnable() {
                public void run() { note = msg(c.scanSystem()); }
            }));
            ty += bh + 8;
            boolean hasPort = cur != null && cur.hasStarport();
            buttons.add(new Btn(bx, ty, bw, bh, "Dock at starport", hasPort, new Runnable() {
                public void run() { note = msg(c.dock()); }
            }));
            ty += bh + 8;
            boolean hasEvent = cur != null && cur.pendingEvent() != null
                    && cur.pendingEvent() != Enums.EventType.NOTHING;
            buttons.add(new Btn(bx, ty, bw, bh, "Resolve encounter", hasEvent, new Runnable() {
                public void run() { eventOpen = true; }
            }));
        }

        for (Btn b : buttons) b.draw(g, mx, my);
        if (!note.isEmpty())
            UiKit.text(g, note, x + 16, y + ph - 10, UiKit.BODY, Palette.ACCENT_WARM);
    }

    private void drawLog(Graphics2D g, Views.GameView v, int x, int y, int wdt, int hgt) {
        UiKit.panel(g, x, y, wdt, hgt);
        UiKit.text(g, "LOG", x + 12, y + 20, UiKit.BODY, Palette.INK_DIM);
        if (v == null || v.log() == null) return;
        List<String> log = v.log();
        int lines = Math.min(4, log.size());
        int ly = y + 40;
        for (int i = log.size() - lines; i < log.size(); i++) {
            UiKit.text(g, log.get(i), x + 12, ly, UiKit.MONO, Palette.INK);
            ly += 17;
        }
    }

    private void drawEventPopup(Graphics2D g, int w, int h, Enums.EventType ev) {
        int pw = 460, ph = 220, x = (w - pw) / 2, y = (h - ph) / 2;
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, w, h);
        UiKit.panel(g, x, y, pw, ph);
        UiKit.textCenter(g, title(ev.name()), x + pw / 2, y + 40, UiKit.H2, Palette.ACCENT);
        UiKit.textCenter(g, eventBlurb(ev), x + pw / 2, y + 74, UiKit.BODY, Palette.INK_DIM);

        String[] opts = eventChoices(ev);
        int bw = pw - 80, bx = x + 40, by = y + 100, bh = 34;
        for (int i = 0; i < opts.length; i++) {
            final int idx = i;
            buttons.add(new Btn(bx, by, bw, bh, opts[i], true, new Runnable() {
                public void run() { note = msg(c.resolveEvent(idx)); eventOpen = false; }
            }));
            Btn last = buttons.get(buttons.size() - 1);
            last.draw(g, mx, my);
            by += bh + 8;
        }
        buttons.add(new Btn(bx, by, bw, 28, "Cancel", true, new Runnable() {
            public void run() { eventOpen = false; }
        }));
        buttons.get(buttons.size() - 1).draw(g, mx, my);
    }

    // ------------------------------------------------------------------ input
    @Override public void mouseMoved(MouseEvent e) { mx = e.getX(); my = e.getY(); }

    @Override public void mousePressed(MouseEvent e) {
        mx = e.getX(); my = e.getY();
        // buttons (built during last render) take priority
        for (int i = buttons.size() - 1; i >= 0; i--) {
            Btn b = buttons.get(i);
            if (b.hit(mx, my)) { b.action.run(); return; }
        }
        if (eventOpen) return;
        // node hit test
        int id = nodeAt(mx, my);
        if (id >= 0) {
            selected = id;
            Views.GameView v = c.view();
            if (v != null && v.galaxy() != null) {
                Views.StarSystemView cur = byId(v.galaxy().systems(), v.galaxy().currentSystem());
                if (cur != null && cur.links() != null && cur.links().contains(id) && id != cur.id()) {
                    doJump(id);
                }
            }
        }
    }

    @Override public void keyPressed(KeyEvent e) {
        if (eventOpen) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) eventOpen = false;
            return;
        }
        int dx = 0, dy = 0;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:  dx = -1; break;
            case KeyEvent.VK_RIGHT: dx = 1;  break;
            case KeyEvent.VK_UP:    dy = -1; break;
            case KeyEvent.VK_DOWN:  dy = 1;  break;
            case KeyEvent.VK_ENTER:
                Views.GameView vv = c.view();
                if (vv != null && vv.galaxy() != null && selected >= 0
                        && selected != vv.galaxy().currentSystem()) doJump(selected);
                return;
            default: return;
        }
        jumpInDirection(dx, dy);
    }

    private void jumpInDirection(int dx, int dy) {
        Views.GameView v = c.view();
        if (v == null || v.galaxy() == null) return;
        Views.StarSystemView cur = byId(v.galaxy().systems(), v.galaxy().currentSystem());
        if (cur == null || cur.links() == null) return;
        int best = -1; double bestDot = 0.15;
        for (Integer lk : cur.links()) {
            if (lk == null) continue;
            Views.StarSystemView s = byId(v.galaxy().systems(), lk.intValue());
            if (s == null) continue;
            double vx = s.x() - cur.x(), vy = s.y() - cur.y();
            double len = Math.sqrt(vx * vx + vy * vy);
            if (len < 1e-6) continue;
            double dot = (vx * dx + vy * dy) / len;
            if (dot > bestDot) { bestDot = dot; best = lk.intValue(); }
        }
        if (best >= 0) { selected = best; doJump(best); }
    }

    private void doJump(int id) {
        ActionResult r = c.jumpTo(id);
        note = msg(r);
        if (r != null && r.isSuccess()) selected = id;
    }

    // ------------------------------------------------------------------ layout helpers
    private void layoutNodes(List<Views.StarSystemView> systems, Views.GalaxyView gal,
                             int x, int y, int w, int h) {
        int n = systems.size();
        if (nodeId.length < n) { nodeId = new int[n]; nodeX = new int[n]; nodeY = new int[n]; }
        nodeCount = n;
        double gw = Math.max(1, gal.width());
        double gh = Math.max(1, gal.height());
        for (int i = 0; i < n; i++) {
            Views.StarSystemView s = systems.get(i);
            nodeId[i] = s.id();
            nodeX[i] = x + (int) (s.x() / gw * w);
            nodeY[i] = y + (int) (s.y() / gh * h);
        }
    }

    private int screenX(int id) {
        for (int i = 0; i < nodeCount; i++) if (nodeId[i] == id) return nodeX[i];
        return Integer.MIN_VALUE;
    }
    private int screenY(int id) {
        for (int i = 0; i < nodeCount; i++) if (nodeId[i] == id) return nodeY[i];
        return Integer.MIN_VALUE;
    }
    private int nodeAt(int px, int py) {
        for (int i = 0; i < nodeCount; i++) {
            int dxp = px - nodeX[i], dyp = py - nodeY[i];
            if (dxp * dxp + dyp * dyp <= 18 * 18) return nodeId[i];
        }
        return -1;
    }

    private static Views.StarSystemView byId(List<Views.StarSystemView> systems, int id) {
        if (systems == null) return null;
        for (Views.StarSystemView s : systems) if (s.id() == id) return s;
        return null;
    }

    private static String msg(ActionResult r) {
        if (r == null) return "";
        return r.message() == null ? "" : r.message();
    }

    private static String title(String enumName) {
        String s = enumName.toLowerCase().replace('_', ' ');
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String eventBlurb(Enums.EventType ev) {
        switch (ev) {
            case DERELICT:      return "A silent hull drifts nearby. Salvage may await inside.";
            case DISTRESS:      return "A faint distress beacon pulses across the void.";
            case MERCHANT:      return "A trader hails you, wares open for business.";
            case HAZARD:        return "Stellar turbulence churns across your path.";
            case PIRATE_AMBUSH: return "Raider signatures decloak on an intercept vector!";
            case STORY:         return "Something significant unfolds here.";
            default:            return "";
        }
    }

    private static String[] eventChoices(Enums.EventType ev) {
        switch (ev) {
            case DERELICT:      return new String[]{ "Board the derelict", "Leave it be" };
            case DISTRESS:      return new String[]{ "Answer the call", "Ignore the signal" };
            case MERCHANT:      return new String[]{ "Open trade", "Decline" };
            case HAZARD:        return new String[]{ "Navigate carefully", "Push through" };
            case PIRATE_AMBUSH: return new String[]{ "Stand and fight", "Attempt to flee" };
            case STORY:         return new String[]{ "Proceed" };
            default:            return new String[]{ "Continue" };
        }
    }
}
