package com.whim.starcommand.ui;

import com.whim.starcommand.app.Game;
import com.whim.starcommand.app.Screen;
import com.whim.starcommand.engine.GroundCombat;
import com.whim.starcommand.model.Character;
import com.whim.starcommand.model.GroundUnit;
import com.whim.starcommand.model.Mission;
import com.whim.starcommand.model.Sector;
import com.whim.starcommand.render.Palette;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Turn-based tactical ground / boarding combat. The player's squad is the
 * living crew; select a unit, move it (arrows or click a reachable tile) and
 * attack adjacent enemies. Reached from a disabled enemy ship (boarding) or by
 * deploying a drop ship onto a hostile planet ("unique area" raid).
 */
public class GroundCombatScreen extends Screen {

    private static final int W = 10;
    private static final int H = 8;

    /** Notified with the outcome when a battle launched with a callback resolves. */
    public interface BattleCallback { void done(boolean won); }

    private GroundCombat battle;
    private GroundUnit selected;
    private Sector sector;
    private boolean boss;
    private boolean boarding;
    private BattleCallback callback;
    private String returnScreen = Game.GALAXY;

    private final GridPanel grid = new GridPanel();
    private final JTextArea log = new JTextArea();
    private JPanel actions;

    public GroundCombatScreen(Game game) {
        super(game);
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(UiKit.label("GROUND / BOARDING COMBAT", UiKit.HEAD, Palette.ACCENT_2), BorderLayout.NORTH);
        add(grid, BorderLayout.CENTER);

        log.setEditable(false);
        log.setFont(UiKit.MONO);
        log.setBackground(Palette.PANEL);
        log.setForeground(Palette.TEXT);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        JScrollPane sc = new JScrollPane(log);
        sc.setPreferredSize(new Dimension(280, 100));
        add(sc, BorderLayout.EAST);

        actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setOpaque(false);
        rebuildActionBar();
        add(actions, BorderLayout.SOUTH);

        Keys.bind(this, "TAB",   new Runnable() { public void run() { cycleSelection(); } });
        Keys.bind(this, "UP",    stepKey(0, -1));
        Keys.bind(this, "DOWN",  stepKey(0, 1));
        Keys.bind(this, "LEFT",  stepKey(-1, 0));
        Keys.bind(this, "RIGHT", stepKey(1, 0));
        Keys.bind(this, "A",     new Runnable() { public void run() { attackNearest(); } });
        Keys.bind(this, "SPACE", new Runnable() { public void run() { endTurn(); } });
    }

    private void rebuildActionBar() {
        actions.removeAll();
        JButton atk = UiKit.button("Attack (A)");
        atk.setFont(UiKit.BODY);
        atk.addActionListener(e -> attackNearest());
        JButton next = UiKit.button("Next unit (Tab)");
        next.setFont(UiKit.BODY);
        next.addActionListener(e -> cycleSelection());
        JButton end = UiKit.button("End turn (Space)");
        end.setFont(UiKit.BODY);
        end.addActionListener(e -> endTurn());
        actions.add(atk);
        actions.add(next);
        actions.add(end);
        actions.revalidate();
        actions.repaint();
    }

    /** Set up a self-contained tactical battle that resolves loot/navigation itself. */
    public void begin(Sector sector, boolean boss, boolean boarding) {
        begin(sector, boss, boarding, Game.GALAXY, null);
    }

    /**
     * Set up a battle whose outcome is handed back to {@code cb} (used by the
     * unique-area crawl, which owns rewards and navigation for room fights).
     */
    public void begin(Sector sector, boolean boss, boolean boarding,
                      String returnScreen, BattleCallback cb) {
        this.sector = sector;
        this.boss = boss;
        this.boarding = boarding;
        this.returnScreen = returnScreen;
        this.callback = cb;
        this.battle = new GroundCombat(game.rng, W, H);
        this.selected = null;
        log.setText("");
        rebuildActionBar();

        int row = 0;
        for (Character c : game.state.crew) {
            if (!c.alive) continue;
            battle.addPlayer(c, 0, Math.min(H - 1, row));
            row += 1;
        }
        if (battle.units.isEmpty()) { // safety: at least one operative
            Character c = game.charGen.roll("Operative", "Marine");
            game.state.crew.add(c);
            battle.addPlayer(c, 0, 0);
        }
        spawnEnemies();
        for (GroundUnit u : battle.units)
            if (u.side == GroundUnit.Side.PLAYER && u.alive()) { selected = u; break; }

        append(boss ? "Boarding party storms Blackbeard's flagship. Take him!"
                    : (boarding ? "Boarding party breaches the enemy hull."
                                : "Drop ship touches down. Secure the area."));
        grid.repaint();
    }

    private void spawnEnemies() {
        int count;
        int hp, acc, minD, maxD;
        String name;
        if (boss) { name = "Pirate"; count = 4; hp = 16; acc = 55; minD = 3; maxD = 7; }
        else if (sector != null && sector.frontier == Sector.Frontier.BETA) {
            name = "Drone"; count = 3; hp = 14; acc = 50; minD = 3; maxD = 6;
        } else { name = "Pirate"; count = 3; hp = 14; acc = 52; minD = 3; maxD = 6; }

        for (int i = 0; i < count; i++) {
            int ex = W - 1 - (i % 2);
            int ey = Math.min(H - 1, i);
            battle.addEnemy(name + " " + (i + 1), ex, ey, hp, acc, minD, maxD, 2, 3);
        }
        if (boss) {
            GroundUnit bb = battle.addEnemy("Blackbeard", W - 1, H / 2, 40, 65, 5, 10, 2, 3);
            bb.name = "Blackbeard";
        }
    }

    private Runnable stepKey(final int dx, final int dy) {
        return new Runnable() { public void run() { stepSelected(dx, dy); } };
    }

    private void stepSelected(int dx, int dy) {
        if (selected == null || battle.result != GroundCombat.Result.ONGOING) return;
        if (battle.move(selected, selected.x + dx, selected.y + dy)) {
            grid.repaint();
        }
    }

    private void cycleSelection() {
        List<GroundUnit> mine = new ArrayList<GroundUnit>();
        for (GroundUnit u : battle.units)
            if (u.side == GroundUnit.Side.PLAYER && u.alive()) mine.add(u);
        if (mine.isEmpty()) return;
        int idx = mine.indexOf(selected);
        selected = mine.get((idx + 1) % mine.size());
        grid.repaint();
    }

    private void attackNearest() {
        if (selected == null || selected.acted) return;
        GroundUnit target = null;
        int bd = Integer.MAX_VALUE;
        for (GroundUnit u : battle.units) {
            if (u.side != GroundUnit.Side.ENEMY || !u.alive()) continue;
            int d = selected.dist(u);
            if (d <= selected.attackRange && d < bd) { bd = d; target = u; }
        }
        if (target == null) { append("No enemy in range of " + selected.name + "."); return; }
        List<String> lines = new ArrayList<String>();
        battle.attack(selected, target, lines);
        for (String l : lines) append(l);
        grid.repaint();
        checkEnd();
    }

    private void endTurn() {
        if (battle.result != GroundCombat.Result.ONGOING) return;
        for (String l : battle.endPlayerTurn()) append(l);
        // reselect a unit that can still act
        selected = null;
        for (GroundUnit u : battle.units)
            if (u.side == GroundUnit.Side.PLAYER && u.alive()) { selected = u; break; }
        grid.repaint();
        checkEnd();
    }

    private void checkEnd() {
        if (battle.result == GroundCombat.Result.ONGOING) return;
        battle.writeBackWounds();

        // Delegated battle (unique-area room fight): hand the result back to the owner.
        if (callback != null) {
            final boolean won = battle.result == GroundCombat.Result.PLAYER_WON;
            if (!won && game.state.livingCrew() == 0) {
                JOptionPane.showMessageDialog(this, "Your entire crew was lost. Game over.");
                finish(Game.MENU);
                return;
            }
            append(won ? "Room secured." : "The squad falls back from this room.");
            final BattleCallback cb = callback;
            actions.removeAll();
            JButton cont = UiKit.button("Continue");
            cont.addActionListener(e -> cb.done(won));
            actions.add(cont);
            actions.revalidate();
            actions.repaint();
            return;
        }

        if (battle.result == GroundCombat.Result.PLAYER_WON) {
            int loot = (boss ? 8000 : 1500) + game.rng.range(200, 900);
            game.state.credits += loot;
            if (sector != null) {
                sector.hostilePresence = false;
                if (sector.planet != null) sector.planet.scanReport = "Cleared by your squad.";
            }
            if (boss) {
                for (Mission m : game.state.missions)
                    if ("m_blackbeard".equals(m.id)) { m.complete = true; game.state.gameWon = true; }
                append("Blackbeard is captured!");
                JOptionPane.showMessageDialog(this,
                        "Blackbeard is in irons. Star Command awards the full bounty.\n"
                        + "The Alpha Frontier is safe. You win!");
            } else {
                append("Area secured. Salvage +" + loot + "cr.");
            }
            finish(Game.GALAXY);
        } else {
            append("The squad is overwhelmed.");
            battle.writeBackWounds();
            if (game.state.livingCrew() == 0) {
                JOptionPane.showMessageDialog(this, "Your entire crew was lost. Game over.");
                finish(Game.MENU);
            } else {
                finish(Game.GALAXY);
            }
        }
    }

    private void finish(final String screen) {
        actions.removeAll();
        JButton cont = UiKit.button("Continue");
        cont.addActionListener(e -> game.screens.show(screen));
        actions.add(cont);
        actions.revalidate();
        actions.repaint();
    }

    private void append(String line) {
        log.append(line + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    @Override
    public String name() { return Game.GROUND; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Palette.SPACE);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    /** The tactical grid: tiles, reachable-move highlight, units, HP pips. */
    private class GridPanel extends JPanel {
        GridPanel() {
            setOpaque(false);
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { onClick(e.getX(), e.getY()); }
            });
        }

        private int cell() {
            int cw = getWidth() / W;
            int ch = getHeight() / H;
            return Math.max(24, Math.min(cw, ch));
        }

        private int originX() { return (getWidth() - cell() * W) / 2; }
        private int originY() { return (getHeight() - cell() * H) / 2; }

        private void onClick(int px, int py) {
            if (battle == null || battle.result != GroundCombat.Result.ONGOING) return;
            int c = cell();
            int gx = (px - originX()) / c;
            int gy = (py - originY()) / c;
            if (!battle.inBounds(gx, gy)) return;
            GroundUnit at = unitAt(gx, gy);
            if (at != null && at.side == GroundUnit.Side.PLAYER) {
                selected = at; repaint(); return;
            }
            if (at != null && at.side == GroundUnit.Side.ENEMY) {
                if (selected != null && selected.dist(at) <= selected.attackRange) {
                    List<String> lines = new ArrayList<String>();
                    battle.attack(selected, at, lines);
                    for (String l : lines) append(l);
                    repaint(); checkEnd();
                }
                return;
            }
            if (selected != null && battle.move(selected, gx, gy)) repaint();
        }

        private GroundUnit unitAt(int x, int y) {
            for (GroundUnit u : battle.units)
                if (u.alive() && u.x == x && u.y == y) return u;
            return null;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (battle == null) return;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int c = cell(), ox = originX(), oy = originY();

            for (int x = 0; x < W; x++) {
                for (int y = 0; y < H; y++) {
                    int px = ox + x * c, py = oy + y * c;
                    g2.setColor(Palette.PANEL);
                    g2.fillRect(px, py, c - 2, c - 2);
                    g2.setColor(Palette.PANEL_LINE);
                    g2.drawRect(px, py, c - 2, c - 2);
                }
            }
            // reachable move tiles for the selected unit
            if (selected != null && !selected.acted) {
                g2.setColor(new Color(90, 200, 255, 60));
                for (int x = 0; x < W; x++)
                    for (int y = 0; y < H; y++)
                        if (Math.abs(selected.x - x) + Math.abs(selected.y - y) <= selected.moveRange
                                && !battle.occupied(x, y))
                            g2.fillRect(ox + x * c, oy + y * c, c - 2, c - 2);
            }
            for (GroundUnit u : battle.units) {
                if (!u.alive()) continue;
                int px = ox + u.x * c, py = oy + u.y * c;
                boolean player = u.side == GroundUnit.Side.PLAYER;
                Color body = player ? Palette.ACCENT : Palette.DANGER;
                if ("Blackbeard".equals(u.name)) body = Palette.ACCENT_2;
                if (u == selected) {
                    g2.setColor(Palette.GOOD);
                    g2.drawRect(px + 1, py + 1, c - 4, c - 4);
                }
                g2.setColor(u.acted && player ? body.darker() : body);
                g2.fillOval(px + c / 4, py + c / 4, c / 2, c / 2);
                // hp bar
                int bw = c - 8;
                g2.setColor(Color.DARK_GRAY);
                g2.fillRect(px + 4, py + c - 8, bw, 4);
                g2.setColor(player ? Palette.GOOD : Palette.DANGER);
                g2.fillRect(px + 4, py + c - 8, (int) ((long) bw * u.hp / Math.max(1, u.maxHp)), 4);
            }
        }
    }
}
