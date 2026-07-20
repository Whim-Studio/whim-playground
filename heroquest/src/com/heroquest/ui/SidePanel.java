package com.heroquest.ui;

import com.heroquest.model.GameState;
import com.heroquest.model.Hero;
import com.heroquest.model.Phase;
import com.heroquest.model.Spell;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.List;

/** Dashboard: active Hero portrait, stats, action buttons and the event log. */
public final class SidePanel extends JPanel {
    private static final Color BG = new Color(0x1E, 0x1B, 0x18);
    private static final Color FG = new Color(0xEC, 0xE4, 0xD8);
    private static final Color ACCENT = new Color(0xC8, 0xA9, 0x6A);

    private final GameController controller;

    private final PortraitPanel portrait = new PortraitPanel();
    private final JLabel nameLabel = label(18, Font.BOLD);
    private final JLabel phaseLabel = label(13, Font.PLAIN);
    private final JLabel bodyLabel = label(14, Font.PLAIN);
    private final JLabel mindLabel = label(14, Font.PLAIN);
    private final JLabel moveLabel = label(14, Font.PLAIN);
    private final JLabel goldLabel = label(14, Font.PLAIN);
    private final JLabel actionLabel = label(13, Font.ITALIC);
    private final JLabel spellsLabel = label(12, Font.PLAIN);

    private final JButton attackBtn = new JButton("Attack");
    private final JButton searchBtn = new JButton("Search");
    private final JButton castBtn = new JButton("Cast Spell");
    private final JButton endBtn = new JButton("End Turn");
    private final JButton newBtn = new JButton("New Quest");

    private final JTextArea logArea = new JTextArea(10, 20);

    public SidePanel(GameController controller) {
        this.controller = controller;
        setBackground(BG);
        setPreferredSize(new Dimension(280, 100));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setLayout(new BorderLayout(0, 10));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildLog(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        attackBtn.addActionListener(e -> controller.toggleAttackMode());
        searchBtn.addActionListener(e -> controller.doSearch());
        castBtn.addActionListener(e -> controller.doCastSpell());
        endBtn.addActionListener(e -> controller.endTurn());
        newBtn.addActionListener(e -> controller.newGame());
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = label(20, Font.BOLD);
        title.setText("HeroQuest");
        title.setForeground(ACCENT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(4));
        phaseLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(phaseLabel);
        header.add(Box.createVerticalStrut(8));

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(portrait, BorderLayout.WEST);

        JPanel stats = new JPanel();
        stats.setOpaque(false);
        stats.setLayout(new BoxLayout(stats, BoxLayout.Y_AXIS));
        stats.add(nameLabel);
        stats.add(bodyLabel);
        stats.add(mindLabel);
        stats.add(moveLabel);
        stats.add(goldLabel);
        row.add(stats, BorderLayout.CENTER);
        header.add(row);
        header.add(Box.createVerticalStrut(6));
        spellsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(spellsLabel);
        actionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(actionLabel);
        return header;
    }

    private JScrollPane buildLog() {
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setBackground(new Color(0x12, 0x10, 0x0E));
        logArea.setForeground(new Color(0xB9, 0xC6, 0xB0));
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane sp = new JScrollPane(logArea);
        sp.setBorder(BorderFactory.createLineBorder(new Color(0x3A, 0x33, 0x2C)));
        return sp;
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.setOpaque(false);
        panel.add(attackBtn);
        panel.add(searchBtn);
        panel.add(castBtn);
        panel.add(endBtn);
        panel.add(newBtn);
        return panel;
    }

    public void refresh() {
        GameState s = controller.getState();
        Hero hero = s.getActiveHero();
        phaseLabel.setText(s.getPhase() == Phase.HERO ? "Hero Phase" : "Zargon's Phase");
        phaseLabel.setForeground(s.getPhase() == Phase.HERO ? new Color(0x8B, 0xC3, 0x4A)
                : new Color(0xE5, 0x73, 0x73));

        portrait.hero = hero;
        if (hero != null) {
            nameLabel.setText(hero.getType().getLabel());
            bodyLabel.setText("Body:  " + hero.getBodyPoints() + " / " + hero.getMaxBodyPoints());
            mindLabel.setText("Mind:  " + hero.getMindPoints() + " / " + hero.getMaxMindPoints());
            moveLabel.setText("Move:  " + s.getMovementRemaining() + " squares left");
            goldLabel.setText("Gold:  " + hero.getGold());
            actionLabel.setText(s.isActionUsed() ? "Action used"
                    : controller.isAttackMode() ? "Select a monster to attack" : "Action available");
            spellsLabel.setText(spellSummary(hero.getSpells()));
        } else {
            nameLabel.setText("—");
            bodyLabel.setText("Body:  —");
            mindLabel.setText("Mind:  —");
            moveLabel.setText("Move:  —");
            goldLabel.setText("Gold:  —");
            actionLabel.setText("");
            spellsLabel.setText("");
        }
        boolean canCast = hero != null && hero.canCastSpells() && !hero.getSpells().isEmpty();
        castBtn.setEnabled(canCast);
        attackBtn.setText(controller.isAttackMode() ? "Cancel Attack" : "Attack");

        rebuildLog(s);
        portrait.repaint();
    }

    private String spellSummary(List<Spell> spells) {
        if (spells == null || spells.isEmpty()) {
            return "Spells: none";
        }
        StringBuilder sb = new StringBuilder("Spells: ");
        for (int i = 0; i < spells.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(spells.get(i).getName());
        }
        return sb.toString();
    }

    private void rebuildLog(GameState s) {
        List<String> log = s.getLog();
        StringBuilder sb = new StringBuilder();
        int from = Math.max(0, log.size() - 40);
        for (int i = from; i < log.size(); i++) {
            sb.append("• ").append(log.get(i)).append('\n');
        }
        logArea.setText(sb.toString());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private static JLabel label(int size, int style) {
        JLabel l = new JLabel();
        l.setForeground(FG);
        l.setFont(new Font("SansSerif", style, size));
        return l;
    }

    /** Small procedurally drawn Hero portrait. */
    private static final class PortraitPanel extends JPanel {
        Hero hero;

        PortraitPanel() {
            setPreferredSize(new Dimension(64, 64));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0x2A, 0x25, 0x1F));
            g2.fillRoundRect(0, 0, 63, 63, 10, 10);
            if (hero == null) {
                return;
            }
            g2.setColor(hero.getColor());
            g2.fillOval(10, 8, 44, 44);
            g2.setColor(new Color(0x20, 0x1A, 0x14));
            g2.fillRoundRect(16, 40, 32, 20, 8, 8);
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("SansSerif", Font.BOLD, 22));
            g2.drawString(hero.getType().getLabel().substring(0, 1), 26, 38);
            g2.setColor(ACCENT);
            g2.drawRoundRect(0, 0, 63, 63, 10, 10);
        }
    }
}
