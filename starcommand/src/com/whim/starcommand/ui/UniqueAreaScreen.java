package com.whim.starcommand.ui;

import com.whim.starcommand.app.Game;
import com.whim.starcommand.app.Screen;
import com.whim.starcommand.engine.UniqueAreaGen;
import com.whim.starcommand.model.Mission;
import com.whim.starcommand.model.Sector;
import com.whim.starcommand.model.UniqueArea;
import com.whim.starcommand.render.Palette;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Unique-area exploration: move a drop-ship squad room to room through a pirate
 * base or hive under fog of war. Enemy rooms hand off to the tactical grid and
 * return here; loot rooms pay out; the objective room is the goal (Blackbeard's
 * stronghold, or a hive core). Reached via "Deploy" on the galaxy map.
 */
public class UniqueAreaScreen extends Screen {

    private UniqueArea area;
    private Sector sector;
    private final MapPanel map = new MapPanel();
    private JLabel status;

    public UniqueAreaScreen(Game game) {
        super(game);
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UiKit.label("UNIQUE AREA", UiKit.HEAD, Palette.ACCENT_2), BorderLayout.WEST);
        status = UiKit.label("", UiKit.BODY, Palette.TEXT_DIM);
        top.add(status, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        add(map, BorderLayout.CENTER);

        JPanel foot = new JPanel(new FlowLayout(FlowLayout.LEFT));
        foot.setOpaque(false);
        foot.add(UiKit.label("Arrows/click: move room   X: extract (at entrance)",
                UiKit.MONO, Palette.TEXT_DIM));
        JButton extract = UiKit.button("Extract (X)");
        extract.addActionListener(e -> extract());
        foot.add(extract);
        add(foot, BorderLayout.SOUTH);

        Keys.bind(this, "UP",    stepKey(-1, 0));
        Keys.bind(this, "DOWN",  stepKey(1, 0));
        Keys.bind(this, "LEFT",  stepKey(0, -1));
        Keys.bind(this, "RIGHT", stepKey(0, 1));
        Keys.bind(this, "X",     new Runnable() { public void run() { extract(); } });
    }

    /** Generate and enter a fresh unique area for the given sector's planet. */
    public void begin(Sector sector, boolean boss) {
        this.sector = sector;
        this.area = new UniqueAreaGen(game.rng).generate(sector.planet, boss);
        refresh();
    }

    private Runnable stepKey(final int dr, final int dc) {
        return new Runnable() { public void run() { tryMove(area.pr + dr, area.pc + dc); } };
    }

    private void tryMove(int r, int c) {
        if (area == null || !area.inBounds(r, c)) return;
        if (!area.canMove(area.pr, area.pc, r, c)) return; // wall between rooms
        area.pr = r;
        area.pc = c;
        UniqueArea.Room room = area.at(r, c);
        room.visited = true;
        area.reveal(r, c);
        resolveRoom(r, c, room);
        refresh();
    }

    private void resolveRoom(int r, int c, UniqueArea.Room room) {
        switch (room.kind) {
            case ENEMY:
                if (!room.cleared) fight(r, c, false);
                break;
            case LOOT:
                if (!room.cleared) {
                    game.state.credits += room.loot;
                    room.cleared = true;
                    JOptionPane.showMessageDialog(this, "Salvage recovered: +" + room.loot + "cr.");
                }
                break;
            case OBJECTIVE:
                if (!room.cleared) fight(r, c, true);
                break;
            default:
                break;
        }
    }

    private void fight(final int r, final int c, final boolean objective) {
        final boolean bossFight = objective && area.boss;
        GroundCombatScreen ground = (GroundCombatScreen) game.screens.get(Game.GROUND);
        ground.begin(sector, bossFight, false, Game.AREA, new GroundCombatScreen.BattleCallback() {
            public void done(boolean won) { onFightResolved(r, c, objective, won); }
        });
        game.screens.show(Game.GROUND);
    }

    private void onFightResolved(int r, int c, boolean objective, boolean won) {
        if (!won) {
            // survivors retreat off-world; the area is left uncleared
            JOptionPane.showMessageDialog(this, "The squad extracts under fire. The area holds.");
            game.screens.show(Game.GALAXY);
            return;
        }
        UniqueArea.Room room = area.at(r, c);
        room.cleared = true;
        if (objective) {
            secureObjective();
        } else {
            game.screens.show(Game.AREA);
        }
    }

    private void secureObjective() {
        if (sector != null) sector.hostilePresence = false;
        if (area.boss) {
            for (Mission m : game.state.missions)
                if ("m_blackbeard".equals(m.id)) { m.complete = true; game.state.gameWon = true; }
            JOptionPane.showMessageDialog(this,
                    "You corner Blackbeard in his stronghold and take him alive.\n"
                    + "Star Command awards the full bounty. The Alpha Frontier is safe — you win!");
        } else {
            int loot = 2000 + game.rng.range(1, 10) * 200;
            game.state.credits += loot;
            JOptionPane.showMessageDialog(this,
                    "Objective secured. The stronghold falls. +" + loot + "cr.");
        }
        game.screens.show(Game.GALAXY);
    }

    private void extract() {
        if (area == null) { game.screens.show(Game.GALAXY); return; }
        if (!area.atEntrance()) {
            JOptionPane.showMessageDialog(this, "Return to the entrance to extract.");
            return;
        }
        if (area.objectiveSecured() && sector != null) sector.hostilePresence = false;
        game.screens.show(Game.GALAXY);
    }

    private void refresh() {
        if (area != null) {
            status.setText(area.title + "    Credits " + game.state.credits
                    + "    Crew " + game.state.livingCrew());
        }
        map.repaint();
    }

    @Override
    public void onShow() { refresh(); requestFocusInWindow(); }

    @Override
    public String name() { return Game.AREA; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Palette.SPACE);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    /** Renders the room grid with fog of war and the squad marker. */
    private class MapPanel extends JPanel {
        MapPanel() {
            setOpaque(false);
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { onClick(e.getX(), e.getY()); }
            });
        }

        private int cell() {
            if (area == null) return 60;
            int cw = getWidth() / area.cols;
            int ch = getHeight() / area.rows;
            return Math.max(48, Math.min(cw, ch));
        }

        private int ox() { return (getWidth() - cell() * area.cols) / 2; }
        private int oy() { return (getHeight() - cell() * area.rows) / 2; }

        private void onClick(int px, int py) {
            if (area == null) return;
            int csz = cell();
            int c = (px - ox()) / csz;
            int r = (py - oy()) / csz;
            tryMove(r, c);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (area == null) return;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int cs = cell(), ox = ox(), oy = oy();
            int inset = Math.max(6, cs / 6);
            int corridor = Math.max(4, inset - 2);

            // undiscovered fog
            for (int r = 0; r < area.rows; r++)
                for (int c = 0; c < area.cols; c++)
                    if (!area.at(r, c).discovered) {
                        int px = ox + c * cs, py = oy + r * cs;
                        g2.setColor(new Color(10, 12, 22));
                        g2.fillRect(px + 2, py + 2, cs - 4, cs - 4);
                    }

            // corridors between discovered rooms (drawn under the chambers' borders)
            for (int r = 0; r < area.rows; r++) {
                for (int c = 0; c < area.cols; c++) {
                    UniqueArea.Room room = area.at(r, c);
                    if (!room.discovered) continue;
                    int px = ox + c * cs, py = oy + r * cs;
                    g2.setColor(Palette.PANEL);
                    if (room.east && area.inBounds(r, c + 1) && area.at(r, c + 1).discovered) {
                        g2.fillRect(px + cs - inset, py + cs / 2 - corridor / 2, inset * 2, corridor);
                    }
                    if (room.south && area.inBounds(r + 1, c) && area.at(r + 1, c).discovered) {
                        g2.fillRect(px + cs / 2 - corridor / 2, py + cs - inset, corridor, inset * 2);
                    }
                }
            }

            // chambers
            for (int r = 0; r < area.rows; r++) {
                for (int c = 0; c < area.cols; c++) {
                    UniqueArea.Room room = area.at(r, c);
                    if (!room.discovered) continue;
                    int px = ox + c * cs + inset, py = oy + r * cs + inset;
                    int sz = cs - 2 * inset;
                    g2.setColor(room.visited ? Palette.PANEL : Palette.PANEL.darker());
                    g2.fillRect(px, py, sz, sz);
                    g2.setColor(Palette.PANEL_LINE);
                    g2.drawRect(px, py, sz, sz);
                    drawIcon(g2, room, px, py, sz);
                }
            }

            // squad marker
            int px = ox + area.pc * cs + cs / 2, py = oy + area.pr * cs + cs / 2;
            g2.setColor(Palette.GOOD);
            g2.fillOval(px - cs / 8, py - cs / 8, cs / 4, cs / 4);
        }

        private void drawIcon(Graphics2D g2, UniqueArea.Room room, int px, int py, int cs) {
            String tag = "";
            Color col = Palette.TEXT_DIM;
            switch (room.kind) {
                case ENTRANCE: tag = "IN";  col = Palette.ACCENT; break;
                case OBJECTIVE: tag = room.cleared ? "DONE" : (area.boss ? "BOSS" : "GOAL");
                                col = room.cleared ? Palette.GOOD : Palette.ACCENT_2; break;
                case ENEMY: tag = room.cleared ? "clear" : "FOE"; col = room.cleared ? Palette.TEXT_DIM : Palette.DANGER; break;
                case LOOT: tag = room.cleared ? "-" : "LOOT"; col = room.cleared ? Palette.TEXT_DIM : Palette.ACCENT; break;
                default: tag = ""; break;
            }
            if (!tag.isEmpty()) {
                g2.setColor(col);
                g2.drawString(tag, px + 10, py + 20);
            }
        }
    }
}
