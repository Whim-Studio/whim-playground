package com.whim.nextrun.ui;

import com.whim.nextrun.domain.Enemy;
import com.whim.nextrun.domain.HeroClass;
import com.whim.nextrun.domain.Player;
import com.whim.nextrun.engine.GameState;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * The full game window: map canvas (center), doom/stat dashboard (right), the
 * context-sensitive action bar (bottom) and a scrolling event log. Movement is
 * driven by the keyboard; every other action by a button. The frame only reads
 * {@link GameState} and dispatches actions to it.
 */
public final class GameFrame extends JFrame {

    private final GameState game;
    private final MapPanel mapPanel;

    private final JLabel doomLabel = new JLabel();
    private final JLabel turnLabel = new JLabel();
    private final JTextArea statsArea = new JTextArea();
    private final JTextArea logArea = new JTextArea();

    private final JButton gatherBtn = action("Gather (g)");
    private final JButton lootBtn   = action("Loot (l)");
    private final JButton exploreBtn= action("Explore (e)");
    private final JButton fightBtn  = action("Fight (f)");
    private final JButton bribeBtn  = action("Bribe (b)");
    private final JButton sneakBtn  = action("Sneak (s)");
    private final JButton weaponBtn = action("Forge Weapon (1)");
    private final JButton armorBtn  = action("Forge Armor (2)");
    private final JButton buildBtn  = action("Build (c)");
    private final JButton restBtn   = action("Rest (r)");

    public GameFrame(GameState game) {
        super("Next Run — " + game.player.heroClass.label());
        this.game = game;
        this.mapPanel = new MapPanel(game);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(6, 6));

        add(wrapMap(), BorderLayout.CENTER);
        add(buildDashboard(), BorderLayout.EAST);
        add(buildActionBar(), BorderLayout.SOUTH);

        wireKeys();
        wireButtons();

        refresh();
        pack();
        setLocationRelativeTo(null);
    }

    private JComponent wrapMap() {
        JScrollPane sp = new JScrollPane(mapPanel);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(new Color(12, 12, 18));
        return sp;
    }

    private JComponent buildDashboard() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(290, 100));
        panel.setBackground(new Color(18, 18, 26));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        doomLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        doomLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        turnLabel.setForeground(new Color(180, 180, 200));
        turnLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        statsArea.setEditable(false);
        statsArea.setBackground(new Color(24, 24, 34));
        statsArea.setForeground(new Color(220, 220, 230));
        statsArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        statsArea.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        statsArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        logArea.setEditable(false);
        logArea.setBackground(new Color(14, 14, 20));
        logArea.setForeground(new Color(200, 210, 190));
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(270, 220));
        logScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(doomLabel);
        panel.add(turnLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(statsArea);
        panel.add(Box.createVerticalStrut(8));
        JLabel logTitle = new JLabel("Chronicle");
        logTitle.setForeground(new Color(150, 160, 180));
        logTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(logTitle);
        panel.add(logScroll);

        return panel;
    }

    private JComponent buildActionBar() {
        JPanel bar = new JPanel(new GridLayout(2, 5, 4, 4));
        bar.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        bar.add(gatherBtn); bar.add(lootBtn);  bar.add(exploreBtn);
        bar.add(restBtn);   bar.add(buildBtn);
        bar.add(fightBtn);  bar.add(bribeBtn);  bar.add(sneakBtn);
        bar.add(weaponBtn); bar.add(armorBtn);
        return bar;
    }

    private static JButton action(String label) {
        JButton b = new JButton(label);
        b.setFocusable(false);
        return b;
    }

    private void wireButtons() {
        gatherBtn.addActionListener(act(new Runnable(){ public void run(){ game.gather(); }}));
        lootBtn.addActionListener(act(new Runnable(){ public void run(){ game.loot(); }}));
        exploreBtn.addActionListener(act(new Runnable(){ public void run(){ game.explore(); }}));
        fightBtn.addActionListener(act(new Runnable(){ public void run(){ game.fight(); }}));
        bribeBtn.addActionListener(act(new Runnable(){ public void run(){ game.bribe(); }}));
        sneakBtn.addActionListener(act(new Runnable(){ public void run(){ game.sneak(); }}));
        weaponBtn.addActionListener(act(new Runnable(){ public void run(){ game.craftWeapon(); }}));
        armorBtn.addActionListener(act(new Runnable(){ public void run(){ game.craftArmor(); }}));
        buildBtn.addActionListener(act(new Runnable(){ public void run(){ game.build(); }}));
        restBtn.addActionListener(act(new Runnable(){ public void run(){ game.rest(); }}));
    }

    private ActionListener act(final Runnable r) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                r.run();
                refresh();
                maybeEnd();
            }
        };
    }

    private void wireKeys() {
        JComponent root = (JComponent) getContentPane();
        bind(root, "UP",    new Runnable(){ public void run(){ game.move(0, -1); }});
        bind(root, "DOWN",  new Runnable(){ public void run(){ game.move(0,  1); }});
        bind(root, "LEFT",  new Runnable(){ public void run(){ game.move(-1, 0); }});
        bind(root, "RIGHT", new Runnable(){ public void run(){ game.move( 1, 0); }});
        bind(root, "W", new Runnable(){ public void run(){ game.move(0, -1); }});
        bind(root, "S", new Runnable(){ public void run(){ game.move(0,  1); }});
        bind(root, "A", new Runnable(){ public void run(){ game.move(-1, 0); }});
        bind(root, "D", new Runnable(){ public void run(){ game.move( 1, 0); }});
        bind(root, "G", new Runnable(){ public void run(){ game.gather(); }});
        bind(root, "L", new Runnable(){ public void run(){ game.loot(); }});
        bind(root, "E", new Runnable(){ public void run(){ game.explore(); }});
        bind(root, "F", new Runnable(){ public void run(){ game.fight(); }});
        bind(root, "B", new Runnable(){ public void run(){ game.bribe(); }});
        bind(root, "shift S", new Runnable(){ public void run(){ game.sneak(); }}); // avoid clash with move-S
        bind(root, "R", new Runnable(){ public void run(){ game.rest(); }});
        bind(root, "C", new Runnable(){ public void run(){ game.build(); }});
        bind(root, "1", new Runnable(){ public void run(){ game.craftWeapon(); }});
        bind(root, "2", new Runnable(){ public void run(){ game.craftArmor(); }});
    }

    private void bind(JComponent root, String key, final Runnable r) {
        Object id = "act_" + key;
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(key), id);
        root.getActionMap().put(id, new javax.swing.AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                r.run();
                refresh();
                maybeEnd();
            }
        });
    }

    private void refresh() {
        int doom = game.turnsUntilWave;
        Color dc = doom <= 3 ? new Color(255, 80, 80)
                 : doom <= 8 ? new Color(255, 180, 60)
                             : new Color(120, 220, 120);
        doomLabel.setForeground(dc);
        doomLabel.setText("Turns until next wave: " + doom);
        turnLabel.setText("Turn " + game.turn + "   |   Waves survived: " + game.waveNumber);

        Player p = game.player;
        StringBuilder sb = new StringBuilder();
        sb.append(p.heroClass.label()).append("\n");
        sb.append("HP   ").append(p.hp).append("/").append(p.maxHp).append("\n");
        sb.append("ATK  ").append(p.attack);
        if (p.weaponBonus > 0) sb.append(" (+").append(p.weaponBonus).append(")");
        sb.append("\n");
        sb.append("DEF  ").append(p.defense);
        if (p.armorBonus > 0) sb.append(" (+").append(p.armorBonus).append(")");
        sb.append("\n");
        sb.append("DEX  ").append(p.dexterity).append("    MAG ").append(p.magic).append("\n");
        sb.append("Gold ").append(p.gold).append("/").append(GameState.GOLD_GOAL).append("\n");
        sb.append("Mats ").append(p.materials).append("\n");
        sb.append("Built ").append(p.structuresBuilt).append("/").append(GameState.BUILD_GOAL).append("\n");
        Enemy adj = game.adjacentEnemy();
        if (adj != null) {
            sb.append("\nAdjacent: ").append(adj.name)
              .append(" (HP ").append(adj.hp)
              .append(", bribe ").append(adj.bribeCost).append("g)");
        }
        statsArea.setText(sb.toString());

        List<String> log = game.log();
        StringBuilder lb = new StringBuilder();
        int from = Math.max(0, log.size() - 40);
        for (int i = from; i < log.size(); i++) lb.append(log.get(i)).append("\n");
        logArea.setText(lb.toString());
        logArea.setCaretPosition(logArea.getDocument().getLength());

        updateButtons();
        mapPanel.repaint();
    }

    private void updateButtons() {
        boolean playing = game.canAct();
        gatherBtn.setEnabled(playing && game.onResource());
        lootBtn.setEnabled(playing && game.onGold());
        exploreBtn.setEnabled(playing && game.onRuin());
        buildBtn.setEnabled(playing && game.onEmpty());
        boolean foe = playing && game.enemyAdjacent();
        fightBtn.setEnabled(foe);
        bribeBtn.setEnabled(foe);
        sneakBtn.setEnabled(foe);
        weaponBtn.setEnabled(playing);
        armorBtn.setEnabled(playing);
        restBtn.setEnabled(playing);
    }

    private boolean ended = false;
    private void maybeEnd() {
        if (ended || game.status == GameState.Status.PLAYING) return;
        ended = true;
        int choice = JOptionPane.showConfirmDialog(this,
            game.outcome + "\n\nPlay again?",
            game.status == GameState.Status.WON ? "Victory" : "Defeat",
            JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() { com.whim.nextrun.app.Main.newRun(); }
            });
        } else {
            System.exit(0);
        }
    }
}
