package com.whim.starcommand.ui;

import com.whim.starcommand.app.Game;
import com.whim.starcommand.app.Screen;
import com.whim.starcommand.engine.CombatEngine;
import com.whim.starcommand.engine.Content;
import com.whim.starcommand.model.Character;
import com.whim.starcommand.model.Mission;
import com.whim.starcommand.model.Sector;
import com.whim.starcommand.model.Ship;
import com.whim.starcommand.model.Weapon;
import com.whim.starcommand.render.Palette;
import com.whim.starcommand.render.ShipSprite;

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

/**
 * Turn-based ship-to-ship combat. The player picks one action per round; the
 * {@link CombatEngine} resolves both sides. Disabling (rather than destroying)
 * the enemy opens a boarding/capture opportunity worth extra credits.
 */
public class ShipCombatScreen extends Screen {

    private Sector sector;
    private boolean boss;
    private Ship enemy;
    private CombatEngine engine;
    private final JTextArea log = new JTextArea();
    private final BattlePanel view = new BattlePanel();
    private JPanel actions;

    public ShipCombatScreen(Game game) {
        super(game);
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(UiKit.label("SHIP-TO-SHIP COMBAT", UiKit.HEAD, Palette.DANGER), BorderLayout.NORTH);
        add(view, BorderLayout.CENTER);

        log.setEditable(false);
        log.setFont(UiKit.MONO);
        log.setBackground(Palette.PANEL);
        log.setForeground(Palette.TEXT);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        JScrollPane sc = new JScrollPane(log);
        sc.setPreferredSize(new Dimension(300, 100));
        add(sc, BorderLayout.EAST);

        actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setOpaque(false);
        addAction("Fire Beam (1)", "1", CombatEngine.Action.FIRE_BEAM);
        addAction("Fire Missile (2)", "2", CombatEngine.Action.FIRE_MISSILE);
        addAction("Shields (3)", "3", CombatEngine.Action.RAISE_SHIELDS);
        addAction("Disable (4)", "4", CombatEngine.Action.DISABLE);
        addAction("Flee (5)", "5", CombatEngine.Action.FLEE);
        add(actions, BorderLayout.SOUTH);
    }

    private void addAction(String text, String key, final CombatEngine.Action action) {
        JButton b = UiKit.button(text);
        b.setFont(UiKit.BODY);
        b.addActionListener(e -> act(action));
        actions.add(b);
        Keys.bind(this, key, new Runnable() { public void run() { act(action); } });
    }

    /** Set up a fresh battle for the given sector. */
    public void begin(Sector s, boolean boss) {
        this.sector = s;
        this.boss = boss;
        this.enemy = makeEnemy(s, boss);
        this.engine = new CombatEngine(game.rng, game.state.ship, enemy, game.captain());
        log.setText("");
        append(boss ? "Blackbeard's flagship looms ahead. This is the one that matters."
                    : "Hostile contact! Enemy " + enemy.className + " closing.");
        setActionsEnabled(true);
        refresh();
    }

    private Ship makeEnemy(Sector s, boolean boss) {
        Ship e;
        if (boss) {
            e = Content.makeShip("Pirate Flagship", 140, 70, 4, 4);
            e.weapons.add(new Weapon("Ion Beam", Weapon.Type.BEAM, 6, 12, 78, 0));
            e.weapons.add(new Weapon("Photon Torpedo", Weapon.Type.MISSILE, 12, 22, 62, 0));
        } else if (s.frontier == Sector.Frontier.BETA) {
            e = Content.makeShip("Hive Drone", 60, 30, 5, 2);
            e.weapons.add(new Weapon("Bio-Plasma", Weapon.Type.BEAM, 5, 10, 72, 0));
        } else {
            e = Content.makeShip("Pirate Raider", 55, 25, 5, 2);
            e.weapons.add(new Weapon("Pulse Laser", Weapon.Type.BEAM, 4, 9, 80, 0));
        }
        return e;
    }

    private void act(CombatEngine.Action action) {
        if (engine == null || engine.result != CombatEngine.Result.ONGOING) return;
        for (String line : engine.round(action)) append(line);
        refresh();
        if (engine.result != CombatEngine.Result.ONGOING) endBattle();
    }

    private void endBattle() {
        setActionsEnabled(false);
        switch (engine.result) {
            case ENEMY_DISABLED: {
                append("Enemy disabled. Launch a boarding party to capture her.");
                JButton board = UiKit.button("Board & capture");
                board.addActionListener(e -> launchBoarding());
                actions.removeAll();
                actions.add(board);
                actions.revalidate();
                actions.repaint();
                break;
            }
            case ENEMY_DESTROYED: {
                int loot = boss ? 4000 : 800;
                sector.hostilePresence = false;
                finishVictory("Enemy destroyed. Bounty +" + loot + "cr.", loot);
                break;
            }
            case PLAYER_FLED:
                append("You escaped. Hostiles remain in this sector.");
                delayReturn(Game.GALAXY);
                break;
            case PLAYER_DESTROYED:
                JOptionPane.showMessageDialog(this, "Your ship was destroyed. Game over.");
                game.screens.show(Game.MENU);
                break;
            default:
                break;
        }
    }

    /** Hand off to tactical ground combat aboard the disabled enemy ship. */
    private void launchBoarding() {
        GroundCombatScreen ground = (GroundCombatScreen) game.screens.get(Game.GROUND);
        ground.begin(sector, boss, true);
        game.screens.show(Game.GROUND);
    }

    private void finishVictory(String msg, int loot) {
        append(msg);
        game.state.credits += loot;
        if (boss) {
            for (Mission m : game.state.missions) {
                if ("m_blackbeard".equals(m.id)) { m.complete = true; game.state.gameWon = true; }
            }
            JOptionPane.showMessageDialog(this,
                    "Blackbeard is captured! Star Command awards you the bounty.\n"
                    + "The Alpha Frontier breathes easier tonight. You win!");
        }
        delayReturn(Game.GALAXY);
    }

    private void delayReturn(final String screen) {
        JButton cont = UiKit.button("Continue");
        cont.addActionListener(e -> game.screens.show(screen));
        actions.removeAll();
        actions.add(cont);
        actions.revalidate();
        actions.repaint();
    }

    private void setActionsEnabled(boolean on) {
        for (java.awt.Component c : actions.getComponents()) c.setEnabled(on);
    }

    private void append(String line) {
        log.append(line + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    private void refresh() { view.repaint(); }

    @Override
    public String name() { return Game.COMBAT; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Palette.SPACE);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    /** Draws both ships and their hull/shield bars. */
    private class BattlePanel extends JPanel {
        BattlePanel() { setOpaque(false); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (engine == null) return;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            Ship p = game.state.ship;

            ShipSprite.draw(g2, 120, h / 2, 40, Palette.ACCENT, false);
            bars(g2, 60, h / 2 + 60, "YOU — " + p.className, p.hull, p.maxHull, p.shield, p.maxShield);

            ShipSprite.draw(g2, w - 160, h / 2, 44, Palette.DANGER, enemy.disabled);
            bars(g2, w - 240, h / 2 + 60, "ENEMY — " + enemy.className,
                    enemy.hull, enemy.maxHull, enemy.shield, enemy.maxShield);
        }

        private void bars(Graphics2D g2, int x, int y, String label,
                          int hull, int maxHull, int shield, int maxShield) {
            g2.setFont(UiKit.MONO);
            g2.setColor(Palette.TEXT);
            g2.drawString(label, x, y - 6);
            bar(g2, x, y, 180, hull, maxHull, Palette.GOOD, "Hull");
            bar(g2, x, y + 20, 180, shield, maxShield, Palette.ACCENT, "Shield");
        }

        private void bar(Graphics2D g2, int x, int y, int w, int val, int max, Color color, String tag) {
            g2.setColor(Palette.PANEL);
            g2.fillRect(x, y, w, 14);
            int fill = max <= 0 ? 0 : (int) ((long) w * Math.max(0, val) / max);
            g2.setColor(color);
            g2.fillRect(x, y, fill, 14);
            g2.setColor(Palette.PANEL_LINE);
            g2.drawRect(x, y, w, 14);
            g2.setColor(Palette.TEXT);
            g2.drawString(tag + " " + Math.max(0, val) + "/" + max, x + w + 8, y + 12);
        }
    }
}
