package com.whim.scg.ui.boarding;

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

/**
 * BOARDING — tile-grid away-mission control.
 *
 * Draws the boarding grid from {@code view().boarding()} (FLOOR/WALL/DOOR/SYSTEM/
 * HAZARD tiles), friendlies + hostiles as tokens at their {@code boardingPos()}.
 * Click a marine to select (or use the view's selectedCrewId), click an adjacent
 * floor tile to move, click an adjacent hostile to attack. Arrow keys move the
 * selected marine. Shows the objective and a win/lose banner, and offers a
 * "Recall party" affordance ({@code endBoarding}).
 *
 * Null-safe: with the stub engine {@code boarding()} is null and the screen shows
 * a graceful "no boarding in progress" state instead of throwing.
 */
public final class BoardingScreen implements Screen {

    private final GameController c;
    private final Starfield stars = new Starfield(70, 23L);

    private int lastW = 1120, lastH = 720;
    private int cell = 36, gridOX = 0, gridOY = 0, gw = 0, gh = 0;

    private int localSelected = -1; // fallback selection when view has none

    private Rectangle recallBtn = new Rectangle();
    private Rectangle backBtn = new Rectangle();

    public BoardingScreen(GameController controller) { this.c = controller; }

    @Override public Enums.Mode mode() { return Enums.Mode.BOARDING; }
    @Override public void onEnter() { localSelected = -1; }
    @Override public void onExit() {}
    @Override public void update(double dt) { stars.update(dt); }

    // ---- rendering ----------------------------------------------------------

    @Override public void render(Graphics2D g, int w, int h) {
        lastW = w; lastH = h;
        UiKit.antialias(g);
        stars.render(g, w, h);

        Views.BoardingView b = c.view().boarding();

        UiKit.panel(g, 12, 8, w - 24, 44);
        UiKit.text(g, "BOARDING PARTY", 24, 36, UiKit.H2, Palette.ACCENT);

        int panelW = Math.min(300, Math.max(230, w / 4));
        int gridAreaX = 12, gridAreaY = 60;
        int gridAreaW = w - panelW - 36;
        int gridAreaH = h - gridAreaY - 52;

        if (b == null) {
            drawNoData(g, gridAreaX, gridAreaY, gridAreaW, gridAreaH);
        } else {
            drawGrid(g, b, gridAreaX, gridAreaY, gridAreaW, gridAreaH);
            drawBanner(g, b, gridAreaX, gridAreaY, gridAreaW);
        }

        drawSidePanel(g, b, w - panelW - 12, 60, panelW, h - 60 - 52);
        drawFooter(g, w, h);
    }

    private void drawNoData(Graphics2D g, int x, int y, int w, int h) {
        UiKit.panel(g, x, y, w, h);
        UiKit.textCenter(g, "no boarding in progress", x + w / 2, y + h / 2 - 8, UiKit.H2, Palette.INK_DIM);
        UiKit.textCenter(g, "begin a boarding action from space combat",
                x + w / 2, y + h / 2 + 16, UiKit.BODY, Palette.INK_DIM);
    }

    private void drawGrid(Graphics2D g, Views.BoardingView b, int ax, int ay, int aw, int ah) {
        UiKit.panel(g, ax, ay, aw, ah);
        gw = Math.max(1, b.gridW());
        gh = Math.max(1, b.gridH());
        int pad = 14;
        cell = Math.max(14, Math.min((aw - pad * 2) / gw, (ah - pad * 2) / gh));
        int usedW = cell * gw, usedH = cell * gh;
        gridOX = ax + (aw - usedW) / 2;
        gridOY = ay + (ah - usedH) / 2;

        for (int y = 0; y < gh; y++) {
            for (int x = 0; x < gw; x++) {
                Enums.TileType t = b.tileAt(new GridPos(x, y));
                int px = gridOX + x * cell, py = gridOY + y * cell;
                g.setColor(tileColor(t));
                g.fillRect(px, py, cell - 1, cell - 1);
                if (t == Enums.TileType.DOOR) {
                    g.setColor(Palette.ACCENT_WARM);
                    g.drawRect(px + 3, py + 3, cell - 7, cell - 7);
                } else if (t == Enums.TileType.SYSTEM) {
                    g.setColor(Palette.ACCENT);
                    g.fillOval(px + cell / 2 - 3, py + cell / 2 - 3, 6, 6);
                } else if (t == Enums.TileType.HAZARD) {
                    g.setColor(Palette.FIRE);
                    g.drawLine(px + 4, py + 4, px + cell - 5, py + cell - 5);
                    g.drawLine(px + cell - 5, py + 4, px + 4, py + cell - 5);
                }
            }
        }
        // lattice
        g.setColor(new Color(0, 0, 0, 60));
        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i <= gw; i++) g.drawLine(gridOX + i * cell, gridOY, gridOX + i * cell, gridOY + usedH);
        for (int j = 0; j <= gh; j++) g.drawLine(gridOX, gridOY + j * cell, gridOX + usedW, gridOY + j * cell);

        int sel = selectedId(b);
        drawTokens(g, b.hostiles(), sel);
        drawTokens(g, b.friendlies(), sel);

        // highlight legal moves for the selected friendly
        Views.CrewView s = findCrew(b.friendlies(), sel);
        if (s != null && s.boardingPos() != null && !b.over()) {
            g.setColor(new Color(Palette.ACCENT.getRed(), Palette.ACCENT.getGreen(), Palette.ACCENT.getBlue(), 90));
            for (Enums.Direction d : Enums.Direction.values()) {
                GridPos n = s.boardingPos().step(d);
                if (n.x < 0 || n.y < 0 || n.x >= gw || n.y >= gh) continue;
                Enums.TileType t = b.tileAt(n);
                if (t == Enums.TileType.FLOOR || t == Enums.TileType.DOOR || t == Enums.TileType.SYSTEM) {
                    g.fillRect(gridOX + n.x * cell + 2, gridOY + n.y * cell + 2, cell - 4, cell - 4);
                }
            }
        }
    }

    private void drawTokens(Graphics2D g, List<Views.CrewView> list, int selId) {
        if (list == null) return;
        int tr = Math.max(8, cell / 2 - 3);
        for (Views.CrewView cv : list) {
            if (cv == null || cv.boardingPos() == null) continue;
            int cx = gridOX + cv.boardingPos().x * cell + cell / 2;
            int cy = gridOY + cv.boardingPos().y * cell + cell / 2;
            CrewCard.token(g, cv, cx, cy, tr, cv.id() == selId);
            if (cv.maxHp() > 0) {
                UiKit.bar(g, cx - tr, cy + tr + 1, tr * 2, 3,
                        (double) cv.hp() / cv.maxHp(), Palette.GOOD, Palette.BREACH);
            }
        }
    }

    private void drawBanner(Graphics2D g, Views.BoardingView b, int ax, int ay, int aw) {
        if (!b.over()) return;
        boolean win = b.playerWon();
        String msg = win ? "OBJECTIVE COMPLETE" : "PARTY LOST";
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(ax, ay + 8, aw, 40);
        UiKit.textCenter(g, msg, ax + aw / 2, ay + 36, UiKit.H2, win ? Palette.GOOD : Palette.BAD);
    }

    private void drawSidePanel(Graphics2D g, Views.BoardingView b, int px, int py, int pw, int ph) {
        UiKit.panel(g, px, py, pw, ph);
        UiKit.text(g, "AWAY TEAM", px + 14, py + 24, UiKit.H2, Palette.INK);

        int y = py + 34;
        if (b != null) {
            // objective
            String obj = b.objective() == null ? "Clear all hostiles" : b.objective();
            g.setColor(Palette.GRID); g.drawLine(px + 12, y, px + pw - 12, y);
            UiKit.text(g, "Objective", px + 14, y + 18, UiKit.MONO, Palette.INK_DIM);
            y += 22;
            y = wrapText(g, obj, px + 14, y + 6, pw - 28, Palette.INK);
            y += 10;

            int hostiles = alive(b.hostiles());
            int friendly = alive(b.friendlies());
            UiKit.text(g, "Marines " + friendly + "   Hostiles " + hostiles,
                    px + 14, y, UiKit.BODY, Palette.INK_DIM);
            y += 18;

            // roster of friendlies
            List<Views.CrewView> fr = b.friendlies();
            int sel = selectedId(b);
            if (fr != null) {
                for (Views.CrewView cv : fr) {
                    if (cv == null) continue;
                    if (y + CrewCard.H > py + ph - 12) break;
                    CrewCard.roster(g, cv, px + 14, y, pw - 28, CrewCard.H, cv.id() == sel);
                    y += CrewCard.H + 8;
                }
            }
        } else {
            UiKit.text(g, "no data", px + 14, y + 14, UiKit.BODY, Palette.INK_DIM);
        }
    }

    private void drawFooter(Graphics2D g, int w, int h) {
        recallBtn = new Rectangle(12, h - 42, 150, 30);
        UiKit.button(g, "Recall party", recallBtn.x, recallBtn.y, recallBtn.width, recallBtn.height, false);
        UiKit.text(g, "click a marine to select  •  click adjacent floor to move  •  click a hostile to attack  •  arrows move",
                180, h - 22, UiKit.BODY, Palette.INK_DIM);
    }

    // ---- input --------------------------------------------------------------

    @Override public void mousePressed(MouseEvent e) {
        int mx = e.getX(), my = e.getY();
        if (recallBtn.contains(mx, my)) { c.endBoarding(); return; }

        Views.BoardingView b = c.view().boarding();
        if (b == null || b.over()) return;

        // click a friendly → select
        Views.CrewView f = crewTokenAt(b.friendlies(), mx, my);
        if (f != null) { localSelected = f.id(); c.selectBoarder(f.id()); return; }

        int sel = selectedId(b);
        Views.CrewView me = findCrew(b.friendlies(), sel);
        if (me == null || me.boardingPos() == null) return;

        // click a hostile → attack if adjacent
        Views.CrewView hostile = crewTokenAt(b.hostiles(), mx, my);
        if (hostile != null && hostile.boardingPos() != null
                && me.boardingPos().manhattan(hostile.boardingPos()) == 1) {
            c.boarderAttack(sel, hostile.id());
            return;
        }

        // click an adjacent tile → move
        GridPos cellPos = pixelToCell(mx, my);
        if (cellPos != null && me.boardingPos().manhattan(cellPos) == 1) {
            c.moveBoarder(sel, cellPos);
        }
    }

    @Override public void keyPressed(KeyEvent e) {
        Views.BoardingView b = c.view().boarding();
        if (b == null || b.over()) return;
        int sel = selectedId(b);
        Views.CrewView me = findCrew(b.friendlies(), sel);
        if (me == null || me.boardingPos() == null) return;

        Enums.Direction d = null;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:    case KeyEvent.VK_W: d = Enums.Direction.NORTH; break;
            case KeyEvent.VK_DOWN:  case KeyEvent.VK_S: d = Enums.Direction.SOUTH; break;
            case KeyEvent.VK_LEFT:  case KeyEvent.VK_A: d = Enums.Direction.WEST;  break;
            case KeyEvent.VK_RIGHT: case KeyEvent.VK_D: d = Enums.Direction.EAST;  break;
            default: return;
        }
        GridPos n = me.boardingPos().step(d);
        if (n.x < 0 || n.y < 0 || n.x >= gw || n.y >= gh) return;
        // if a hostile occupies the target, attack; else attempt move
        Views.CrewView occ = crewAt(b.hostiles(), n);
        if (occ != null) c.boarderAttack(sel, occ.id());
        else c.moveBoarder(sel, n);
    }

    // ---- helpers ------------------------------------------------------------

    private int selectedId(Views.BoardingView b) {
        int s = b == null ? -1 : b.selectedCrewId();
        if (s >= 0) return s;
        if (localSelected >= 0) return localSelected;
        // default to first living friendly
        if (b != null && b.friendlies() != null) {
            for (Views.CrewView cv : b.friendlies())
                if (cv != null && cv.alive()) return cv.id();
        }
        return -1;
    }

    private GridPos pixelToCell(int mx, int my) {
        if (cell <= 0 || mx < gridOX || my < gridOY) return null;
        int x = (mx - gridOX) / cell, y = (my - gridOY) / cell;
        if (x < 0 || y < 0 || x >= gw || y >= gh) return null;
        return new GridPos(x, y);
    }

    private Views.CrewView crewTokenAt(List<Views.CrewView> list, int mx, int my) {
        GridPos p = pixelToCell(mx, my);
        if (p == null) return null;
        return crewAt(list, p);
    }

    private Views.CrewView crewAt(List<Views.CrewView> list, GridPos p) {
        if (list == null || p == null) return null;
        for (Views.CrewView cv : list)
            if (cv != null && cv.alive() && p.equals(cv.boardingPos())) return cv;
        return null;
    }

    private Views.CrewView findCrew(List<Views.CrewView> list, int id) {
        if (list == null || id < 0) return null;
        for (Views.CrewView cv : list) if (cv != null && cv.id() == id) return cv;
        return null;
    }

    private int alive(List<Views.CrewView> list) {
        if (list == null) return 0;
        int n = 0;
        for (Views.CrewView cv : list) if (cv != null && cv.alive()) n++;
        return n;
    }

    private int wrapText(Graphics2D g, String s, int x, int y, int w, Color col) {
        g.setFont(UiKit.BODY);
        g.setColor(col);
        String[] words = s.split(" ");
        StringBuilder line = new StringBuilder();
        int ly = y;
        for (String word : words) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (g.getFontMetrics().stringWidth(test) > w && line.length() > 0) {
                g.drawString(line.toString(), x, ly);
                ly += 16;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) { g.drawString(line.toString(), x, ly); ly += 16; }
        return ly;
    }

    private static Color tileColor(Enums.TileType t) {
        if (t == null) return Palette.BREACH;
        switch (t) {
            case FLOOR:  return new Color(0x1E2636);
            case WALL:   return new Color(0x0C111B);
            case DOOR:   return new Color(0x243247);
            case SYSTEM: return new Color(0x243a3a);
            case HAZARD: return new Color(0x3a2420);
            default:     return Palette.BREACH;
        }
    }
}
