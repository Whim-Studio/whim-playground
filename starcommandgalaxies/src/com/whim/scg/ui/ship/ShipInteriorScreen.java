package com.whim.scg.ui.ship;

import com.whim.scg.api.Enums;
import com.whim.scg.api.GameController;
import com.whim.scg.api.GridPos;
import com.whim.scg.api.Screen;
import com.whim.scg.api.Views;
import com.whim.scg.render.Palette;
import com.whim.scg.render.Starfield;
import com.whim.scg.render.UiKit;
import com.whim.scg.ui.crew.CrewCard;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

/**
 * SHIP_INTERIOR — the home/hub screen after New Game.
 *
 * Draws the ship room grid from {@code view().playerShip()} with power dots,
 * integrity, fire/breach state and stationed crew tokens. Supports drag-and-drop
 * crew assignment (press a crew token, drag onto a room), a per-room power
 * allocation UI, and a crew roster panel with rename / role-change. A "To Galaxy"
 * affordance switches to GALAXY_MAP.
 *
 * Renders defensively: with the stub engine {@code playerShip()} is null and the
 * screen shows a graceful "awaiting engine" state instead of throwing.
 */
public final class ShipInteriorScreen implements Screen {

    private final GameController c;
    private final Starfield stars = new Starfield(90, 17L);

    // layout cache (set each render, reused by mouse handlers)
    private int lastW = 1120, lastH = 720;
    private int cell = 40, gridOX = 0, gridOY = 0;

    // interaction state
    private int selectedCrewId = -1;
    private int selectedRoomId = -1;
    private int hoverRoomId = -1;

    // drag-and-drop
    private int dragCrewId = -1;
    private int dragX, dragY;
    private boolean dragging;

    // clickable regions (recomputed each render)
    private Rectangle toGalaxyBtn = new Rectangle();
    private Rectangle renameBtn = new Rectangle();
    private Rectangle roleBtn = new Rectangle();
    private Rectangle powMinusBtn = new Rectangle();
    private Rectangle powPlusBtn = new Rectangle();
    private final List<int[]> rosterHits = new ArrayList<int[]>(); // {crewId, x, y, w, h}

    public ShipInteriorScreen(GameController controller) { this.c = controller; }

    @Override public Enums.Mode mode() { return Enums.Mode.SHIP_INTERIOR; }
    @Override public void onEnter() { selectedRoomId = -1; }
    @Override public void onExit() { dragging = false; dragCrewId = -1; }
    @Override public void update(double dt) { stars.update(dt); }

    // ---- rendering ----------------------------------------------------------

    @Override public void render(Graphics2D g, int w, int h) {
        lastW = w; lastH = h;
        UiKit.antialias(g);
        stars.render(g, w, h);

        Views.ShipView ship = c.view().playerShip();
        drawHeader(g, w, ship);

        int panelW = Math.min(300, Math.max(240, w / 4));
        int gridAreaX = 12;
        int gridAreaY = 64;
        int gridAreaW = w - panelW - 36;
        int gridAreaH = h - gridAreaY - 52;

        if (ship == null) {
            drawNoData(g, gridAreaX, gridAreaY, gridAreaW, gridAreaH);
        } else {
            drawGrid(g, ship, gridAreaX, gridAreaY, gridAreaW, gridAreaH);
        }

        drawRosterPanel(g, ship, w - panelW - 12, 64, panelW, h - 64 - 52);
        drawFooter(g, w, h);

        if (dragging && dragCrewId >= 0 && ship != null) {
            Views.CrewView cv = crewById(ship, dragCrewId);
            if (cv != null) CrewCard.token(g, cv, dragX, dragY, 12, true);
        }
    }

    private void drawHeader(Graphics2D g, int w, Views.ShipView ship) {
        UiKit.panel(g, 12, 8, w - 24, 48);
        String name = ship == null ? "SCV (no telemetry)" : ship.name();
        UiKit.text(g, name, 24, 38, UiKit.H2, Palette.ACCENT);

        int x = 260;
        if (ship != null) {
            x = stat(g, x, "HULL", ship.hull(), ship.maxHull(), Palette.HULL);
            x = stat(g, x, "SHLD", ship.shields(), ship.maxShields(), Palette.SHIELD);
            x = stat(g, x, "O2", ship.oxygen(), 100, Palette.ACCENT);
            x = stat(g, x, "PWR", ship.reactor() - ship.reactorUsed(), ship.reactor(), Palette.ACCENT_WARM);
        }
        String right = "Credits " + c.view().credits() + "    Day " + c.view().day();
        g.setFont(UiKit.BODY);
        int rw = g.getFontMetrics().stringWidth(right);
        UiKit.text(g, right, w - rw - 24, 38, UiKit.BODY, Palette.INK);
    }

    private int stat(Graphics2D g, int x, String label, int val, int max, Color col) {
        UiKit.text(g, label, x, 26, UiKit.MONO, Palette.INK_DIM);
        UiKit.bar(g, x, 32, 70, 7, max > 0 ? (double) val / max : 0, col, Palette.BREACH);
        UiKit.text(g, val + "/" + max, x, 48, UiKit.MONO, Palette.INK_DIM);
        return x + 92;
    }

    private void drawNoData(Graphics2D g, int x, int y, int w, int h) {
        UiKit.panel(g, x, y, w, h);
        UiKit.textCenter(g, "awaiting engine", x + w / 2, y + h / 2 - 10, UiKit.H2, Palette.INK_DIM);
        UiKit.textCenter(g, "start a New Game once the simulation is loaded",
                x + w / 2, y + h / 2 + 16, UiKit.BODY, Palette.INK_DIM);
    }

    private void drawGrid(Graphics2D g, Views.ShipView ship, int ax, int ay, int aw, int ah) {
        UiKit.panel(g, ax, ay, aw, ah);
        int gw = Math.max(1, ship.gridW());
        int gh = Math.max(1, ship.gridH());
        int pad = 16;
        cell = Math.max(16, Math.min((aw - pad * 2) / gw, (ah - pad * 2) / gh));
        int usedW = cell * gw, usedH = cell * gh;
        gridOX = ax + (aw - usedW) / 2;
        gridOY = ay + (ah - usedH) / 2;

        // faint cell lattice
        g.setColor(Palette.GRID);
        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i <= gw; i++)
            g.drawLine(gridOX + i * cell, gridOY, gridOX + i * cell, gridOY + usedH);
        for (int j = 0; j <= gh; j++)
            g.drawLine(gridOX, gridOY + j * cell, gridOX + usedW, gridOY + j * cell);

        List<Views.RoomView> rooms = ship.rooms();
        if (rooms != null) {
            for (Views.RoomView r : rooms) drawRoom(g, r);
        }
        drawCrewTokens(g, ship);

        if (selectedRoomId >= 0) drawRoomInspector(g, ship, ax, ay, aw, ah);
    }

    private void drawRoom(Graphics2D g, Views.RoomView r) {
        if (r == null || r.origin() == null) return;
        int rx = gridOX + r.origin().x * cell;
        int ry = gridOY + r.origin().y * cell;
        int rw = Math.max(1, r.w()) * cell;
        int rh = Math.max(1, r.h()) * cell;

        g.setColor(Palette.room(r.type()));
        g.fillRect(rx + 1, ry + 1, rw - 2, rh - 2);

        // breach darkens, fire glows
        if (r.breached()) {
            g.setColor(new Color(0, 0, 0, 130));
            g.fillRect(rx + 1, ry + 1, rw - 2, rh - 2);
        }
        if (r.onFire()) {
            g.setColor(new Color(Palette.FIRE.getRed(), Palette.FIRE.getGreen(), Palette.FIRE.getBlue(), 110));
            g.fillRect(rx + 1, ry + 1, rw - 2, rh - 2);
        }

        boolean sel = r.id() == selectedRoomId;
        boolean hov = r.id() == hoverRoomId;
        g.setColor(sel ? Palette.INK : hov ? Palette.ACCENT : Palette.GRID);
        g.setStroke(new BasicStroke(sel ? 2.5f : 1.5f));
        g.drawRect(rx + 1, ry + 1, rw - 2, rh - 2);

        // label
        UiKit.text(g, shortType(r.type()), rx + 5, ry + 15, UiKit.MONO, Palette.INK);

        // integrity bar along the bottom
        if (r.maxHp() > 0) {
            double f = (double) r.hp() / r.maxHp();
            Color ic = f > 0.5 ? Palette.GOOD : f > 0.25 ? Palette.ACCENT_WARM : Palette.BAD;
            UiKit.bar(g, rx + 4, ry + rh - 8, rw - 8, 4, f, ic, Palette.BREACH);
        }

        // power dots (top-right)
        if (r.type() != null && r.type().isPowered() && r.maxPower() > 0) {
            int dots = r.maxPower();
            int dr = 4;
            int dx0 = rx + rw - 6 - dots * (dr + 2);
            for (int i = 0; i < dots; i++) {
                int dcx = dx0 + i * (dr + 2) + dr;
                int dcy = ry + 8;
                g.setColor(i < r.power() ? Palette.ACCENT_WARM : Palette.BREACH);
                g.fillOval(dcx - dr, dcy - dr, dr * 2, dr * 2);
            }
        }

        // status glyphs
        if (r.onFire())  UiKit.text(g, "FIRE",   rx + 5, ry + rh - 12, UiKit.MONO, Palette.FIRE);
        if (r.breached())UiKit.text(g, "BREACH", rx + 5, ry + rh - 12, UiKit.MONO, Palette.BAD);
    }

    private void drawCrewTokens(Graphics2D g, Views.ShipView ship) {
        List<Views.CrewView> crew = ship.crew();
        if (crew == null) return;
        // count crew per room to lay tokens out in a row
        for (Views.RoomView r : safeRooms(ship)) {
            List<Views.CrewView> here = new ArrayList<Views.CrewView>();
            for (Views.CrewView cv : crew) {
                if (cv != null && cv.stationRoomId() == r.id() && cv.id() != dragCrewId) here.add(cv);
            }
            if (here.isEmpty() || r.origin() == null) continue;
            int rx = gridOX + r.origin().x * cell;
            int ry = gridOY + r.origin().y * cell;
            int rw = Math.max(1, r.w()) * cell;
            int rh = Math.max(1, r.h()) * cell;
            int tr = Math.max(7, Math.min(12, cell / 3));
            int gap = tr * 2 + 3;
            int perRow = Math.max(1, (rw - 6) / gap);
            for (int i = 0; i < here.size(); i++) {
                int col = i % perRow, row = i / perRow;
                int tx = rx + 6 + tr + col * gap;
                int ty = ry + rh - 6 - tr - row * gap;
                CrewCard.token(g, here.get(i), tx, ty, tr, here.get(i).id() == selectedCrewId);
            }
        }
    }

    private void drawRoomInspector(Graphics2D g, Views.ShipView ship, int ax, int ay, int aw, int ah) {
        Views.RoomView r = roomById(ship, selectedRoomId);
        if (r == null) return;
        int pw = 200, ph = 92;
        int px = ax + aw - pw - 10, py = ay + ah - ph - 10;
        UiKit.panel(g, px, py, pw, ph);
        UiKit.text(g, typeName(r.type()), px + 12, py + 22, UiKit.H2, Palette.INK);
        UiKit.text(g, "Integrity " + r.hp() + "/" + r.maxHp(), px + 12, py + 42, UiKit.BODY, Palette.INK_DIM);

        boolean powered = r.type() != null && r.type().isPowered() && r.maxPower() > 0;
        if (powered) {
            UiKit.text(g, "Power " + r.power() + "/" + r.maxPower(), px + 12, py + 64, UiKit.BODY, Palette.INK_DIM);
            powMinusBtn = new Rectangle(px + 108, py + 50, 24, 20);
            powPlusBtn  = new Rectangle(px + 140, py + 50, 24, 20);
            UiKit.button(g, "-", powMinusBtn.x, powMinusBtn.y, powMinusBtn.width, powMinusBtn.height, false);
            UiKit.button(g, "+", powPlusBtn.x, powPlusBtn.y, powPlusBtn.width, powPlusBtn.height, false);
        } else {
            powMinusBtn = new Rectangle(); powPlusBtn = new Rectangle();
            UiKit.text(g, "unpowered section", px + 12, py + 64, UiKit.BODY, Palette.INK_DIM);
        }
    }

    // ---- roster panel -------------------------------------------------------

    private void drawRosterPanel(Graphics2D g, Views.ShipView ship, int px, int py, int pw, int ph) {
        UiKit.panel(g, px, py, pw, ph);
        UiKit.text(g, "CREW ROSTER", px + 14, py + 24, UiKit.H2, Palette.INK);
        rosterHits.clear();

        if (ship == null || ship.crew() == null || ship.crew().isEmpty()) {
            UiKit.text(g, "no crew aboard", px + 14, py + 50, UiKit.BODY, Palette.INK_DIM);
            renameBtn = new Rectangle(); roleBtn = new Rectangle();
            return;
        }

        List<Views.CrewView> crew = ship.crew();
        int cardW = pw - 28;
        int cardH = 78;
        int y = py + 36;
        int detailH = selectedCrewId >= 0 ? 64 : 0;
        int maxY = py + ph - detailH - 12;
        for (Views.CrewView cv : crew) {
            if (cv == null) continue;
            if (y + cardH > maxY) break; // clip to panel
            boolean hot = cv.id() == selectedCrewId;
            CrewCard.roster(g, cv, px + 14, y, cardW, cardH, hot);
            rosterHits.add(new int[]{cv.id(), px + 14, y, cardW, cardH});
            y += cardH + 8;
        }

        if (selectedCrewId >= 0) drawCrewActions(g, ship, px, py + ph - detailH - 2, pw, detailH);
        else { renameBtn = new Rectangle(); roleBtn = new Rectangle(); }
    }

    private void drawCrewActions(Graphics2D g, Views.ShipView ship, int px, int py, int pw, int ph) {
        Views.CrewView cv = crewById(ship, selectedCrewId);
        if (cv == null) { renameBtn = new Rectangle(); roleBtn = new Rectangle(); return; }
        g.setColor(Palette.GRID);
        g.drawLine(px + 12, py, px + pw - 12, py);
        UiKit.text(g, "Selected: " + cv.name(), px + 14, py + 20, UiKit.BODY, Palette.ACCENT);

        int bw = (pw - 28 - 10) / 2;
        renameBtn = new Rectangle(px + 14, py + 30, bw, 26);
        roleBtn = new Rectangle(px + 14 + bw + 10, py + 30, bw, 26);
        UiKit.button(g, "Rename", renameBtn.x, renameBtn.y, renameBtn.width, renameBtn.height, false);
        UiKit.button(g, "Role ▸", roleBtn.x, roleBtn.y, roleBtn.width, roleBtn.height, false);
    }

    private void drawFooter(Graphics2D g, int w, int h) {
        toGalaxyBtn = new Rectangle(12, h - 42, 150, 30);
        UiKit.button(g, "To Galaxy ▸", toGalaxyBtn.x, toGalaxyBtn.y, toGalaxyBtn.width, toGalaxyBtn.height, false);
        UiKit.text(g, "drag crew to a room to station them  •  click a room to allocate power  •  Esc: menu",
                180, h - 22, UiKit.BODY, Palette.INK_DIM);
    }

    // ---- input --------------------------------------------------------------

    @Override public void mousePressed(MouseEvent e) {
        int mx = e.getX(), my = e.getY();

        if (toGalaxyBtn.contains(mx, my)) { c.setMode(Enums.Mode.GALAXY_MAP); return; }

        if (selectedCrewId >= 0 && renameBtn.contains(mx, my)) { doRename(); return; }
        if (selectedCrewId >= 0 && roleBtn.contains(mx, my)) { doCycleRole(); return; }
        if (selectedRoomId >= 0 && powMinusBtn.contains(mx, my)) { nudgePower(-1); return; }
        if (selectedRoomId >= 0 && powPlusBtn.contains(mx, my)) { nudgePower(+1); return; }

        // roster card → select + begin drag
        for (int[] hit : rosterHits) {
            if (mx >= hit[1] && mx <= hit[1] + hit[3] && my >= hit[2] && my <= hit[2] + hit[4]) {
                selectedCrewId = hit[0];
                beginDrag(hit[0], mx, my);
                return;
            }
        }

        Views.ShipView ship = c.view().playerShip();
        if (ship == null) return;

        // crew token on the grid → select + begin drag
        int tok = crewTokenAt(ship, mx, my);
        if (tok >= 0) { selectedCrewId = tok; beginDrag(tok, mx, my); return; }

        // room click → select room
        Views.RoomView r = roomAtPixel(ship, mx, my);
        selectedRoomId = r == null ? -1 : r.id();
    }

    @Override public void mouseDragged(MouseEvent e) {
        if (dragging) { dragX = e.getX(); dragY = e.getY(); }
    }

    @Override public void mouseReleased(MouseEvent e) {
        if (!dragging) return;
        dragging = false;
        int crewId = dragCrewId;
        dragCrewId = -1;
        Views.ShipView ship = c.view().playerShip();
        if (ship == null || crewId < 0) return;
        Views.RoomView r = roomAtPixel(ship, e.getX(), e.getY());
        if (r != null) c.assignCrew(crewId, r.id());
    }

    @Override public void mouseMoved(MouseEvent e) {
        Views.ShipView ship = c.view().playerShip();
        hoverRoomId = -1;
        if (ship == null) return;
        Views.RoomView r = roomAtPixel(ship, e.getX(), e.getY());
        hoverRoomId = r == null ? -1 : r.id();
    }

    @Override public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_G: c.setMode(Enums.Mode.GALAXY_MAP); break;
            case KeyEvent.VK_R: if (selectedCrewId >= 0) doRename(); break;
            case KeyEvent.VK_S: c.save("auto"); break;
            default: break;
        }
    }

    // ---- intents / helpers --------------------------------------------------

    private void beginDrag(int crewId, int mx, int my) {
        dragCrewId = crewId; dragging = true; dragX = mx; dragY = my;
    }

    private void nudgePower(int delta) {
        Views.ShipView ship = c.view().playerShip();
        Views.RoomView r = roomById(ship, selectedRoomId);
        if (r == null) return;
        int next = Math.max(0, Math.min(r.maxPower(), r.power() + delta));
        c.setRoomPower(r.id(), next);
    }

    private void doRename() {
        Views.ShipView ship = c.view().playerShip();
        Views.CrewView cv = crewById(ship, selectedCrewId);
        if (cv == null) return;
        String name = JOptionPane.showInputDialog(null, "New name for " + cv.name() + ":", cv.name());
        if (name != null && !name.trim().isEmpty()) c.renameCrew(selectedCrewId, name.trim());
    }

    private void doCycleRole() {
        Views.ShipView ship = c.view().playerShip();
        Views.CrewView cv = crewById(ship, selectedCrewId);
        if (cv == null || cv.role() == null) return;
        Enums.CrewRole[] roles = Enums.CrewRole.values();
        int next = (cv.role().ordinal() + 1) % roles.length;
        c.setRole(selectedCrewId, roles[next]);
    }

    // ---- geometry / lookups -------------------------------------------------

    private Views.RoomView roomAtPixel(Views.ShipView ship, int mx, int my) {
        if (cell <= 0) return null;
        int gx = (mx - gridOX) / cell;
        int gy = (my - gridOY) / cell;
        if (mx < gridOX || my < gridOY) return null;
        if (gx < 0 || gy < 0 || gx >= ship.gridW() || gy >= ship.gridH()) return null;
        GridPos p = new GridPos(gx, gy);
        for (Views.RoomView r : safeRooms(ship)) {
            if (r.contains(p)) return r;
        }
        return null;
    }

    private int crewTokenAt(Views.ShipView ship, int mx, int my) {
        List<Views.CrewView> crew = ship.crew();
        if (crew == null) return -1;
        for (Views.RoomView r : safeRooms(ship)) {
            if (r.origin() == null) continue;
            List<Views.CrewView> here = new ArrayList<Views.CrewView>();
            for (Views.CrewView cv : crew)
                if (cv != null && cv.stationRoomId() == r.id()) here.add(cv);
            if (here.isEmpty()) continue;
            int rx = gridOX + r.origin().x * cell;
            int ry = gridOY + r.origin().y * cell;
            int rw = Math.max(1, r.w()) * cell;
            int rh = Math.max(1, r.h()) * cell;
            int tr = Math.max(7, Math.min(12, cell / 3));
            int gap = tr * 2 + 3;
            int perRow = Math.max(1, (rw - 6) / gap);
            for (int i = 0; i < here.size(); i++) {
                int col = i % perRow, row = i / perRow;
                int tx = rx + 6 + tr + col * gap;
                int ty = ry + rh - 6 - tr - row * gap;
                if ((mx - tx) * (mx - tx) + (my - ty) * (my - ty) <= (tr + 2) * (tr + 2))
                    return here.get(i).id();
            }
        }
        return -1;
    }

    private List<Views.RoomView> safeRooms(Views.ShipView ship) {
        List<Views.RoomView> rooms = ship.rooms();
        if (rooms == null) return new ArrayList<Views.RoomView>();
        return rooms;
    }

    private Views.RoomView roomById(Views.ShipView ship, int id) {
        if (ship == null) return null;
        for (Views.RoomView r : safeRooms(ship)) if (r != null && r.id() == id) return r;
        return null;
    }

    private Views.CrewView crewById(Views.ShipView ship, int id) {
        if (ship == null || ship.crew() == null) return null;
        for (Views.CrewView cv : ship.crew()) if (cv != null && cv.id() == id) return cv;
        return null;
    }

    private static String shortType(Enums.RoomType t) {
        if (t == null) return "";
        switch (t) {
            case BRIDGE: return "BRDG";
            case ENGINES: return "ENG";
            case WEAPONS: return "WPN";
            case SHIELDS: return "SHLD";
            case MEDBAY: return "MED";
            case TELEPORTER: return "TELE";
            case OXYGEN: return "O2";
            case SENSORS: return "SENS";
            case QUARTERS: return "QTRS";
            case CARGO: return "CRGO";
            default: return "CORR";
        }
    }

    private static String typeName(Enums.RoomType t) {
        if (t == null) return "Room";
        String s = t.name().toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
