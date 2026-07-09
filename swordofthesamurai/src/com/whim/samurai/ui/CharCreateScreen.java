package com.whim.samurai.ui;

import com.whim.samurai.app.Game;
import com.whim.samurai.app.Screen;
import com.whim.samurai.model.Samurai;
import com.whim.samurai.render.Palette;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * "Take Up the Sword" — character creation (design ref §4 / §7.2). You begin as a
 * young samurai of a leading family: enter a clan (family) name and a given name,
 * distribute a small pool of points across the three arcade skills (swordsmanship,
 * generalship, stealth), and preview your starting honour / power / koku before
 * committing. Confirm builds the world via {@link Game#newGame} and stamps your
 * chosen name and skills onto the generated player.
 */
public class CharCreateScreen extends Screen {

    // Each skill starts at a base and shares a pool (approximation of the manual's
    // "family advantage" edge in one attribute, design ref §7.2).
    private static final int BASE = 5;
    private static final int POOL = 12;
    private static final int MAX_PER = 14;

    // Starting reputation/economy shown as a preview — mirrors WorldGen's baseline.
    private static final int START_HONOR = 120;
    private static final int START_POWER = 40;
    private static final int START_KOKU  = 200;

    private final JTextField clanField = new JTextField("Oda", 12);
    private final JTextField givenField = new JTextField("Nobunaga", 12);

    private int sword = BASE, general = BASE, stealth = BASE;
    private final JLabel swordVal   = plainLabel();
    private final JLabel generalVal = plainLabel();
    private final JLabel stealthVal = plainLabel();
    private final JLabel poolLabel  = plainLabel();

    public CharCreateScreen(Game game) {
        super(game);
        setLayout(new GridBagLayout());

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        form.add(fieldRow("Family / Clan name:", clanField));
        form.add(Box.createVerticalStrut(10));
        form.add(fieldRow("Given name:", givenField));
        form.add(Box.createVerticalStrut(18));

        form.add(left(UiKit.label("Allot your training  (“family advantage”):", UiKit.HEAD, Palette.INK)));
        form.add(Box.createVerticalStrut(8));
        form.add(skillRow("Swordsmanship  (duels & melee)", 0));
        form.add(skillRow("Generalship  (open-field battle)", 1));
        form.add(skillRow("Stealth  (ninja & infiltration)", 2));
        form.add(Box.createVerticalStrut(6));
        form.add(left(poolLabel));
        form.add(Box.createVerticalStrut(20));

        JButton confirm = UiKit.button("Take Up the Sword");
        confirm.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirm.addActionListener(e -> confirm());
        JButton back = UiKit.button("Back");
        back.setAlignmentX(Component.LEFT_ALIGNMENT);
        back.addActionListener(e -> game.screens.show(Game.MENU));

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttons.add(confirm);
        buttons.add(Box.createHorizontalStrut(12));
        buttons.add(back);
        form.add(buttons);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0;
        gc.insets = new Insets(150, 0, 0, 0);
        add(form, gc);

        Keys.bind(this, "ESCAPE", () -> game.screens.show(Game.MENU));
        refresh();
    }

    public String name() { return Game.CREATE; }

    private int remaining() { return POOL - (sword - BASE) - (general - BASE) - (stealth - BASE); }

    private void adjust(int which, int delta) {
        int cur = which == 0 ? sword : which == 1 ? general : stealth;
        int next = cur + delta;
        if (next < BASE || next > MAX_PER) return;
        if (delta > 0 && remaining() <= 0) return;
        if (which == 0) sword = next; else if (which == 1) general = next; else stealth = next;
        refresh();
    }

    private void refresh() {
        swordVal.setText(String.valueOf(sword));
        generalVal.setText(String.valueOf(general));
        stealthVal.setText(String.valueOf(stealth));
        poolLabel.setText("Points remaining: " + remaining());
        repaint();
    }

    private void confirm() {
        String clan = clanField.getText().trim();
        String given = givenField.getText().trim();
        if (clan.isEmpty()) clan = "Oda";
        if (given.isEmpty()) given = "Nobunaga";
        // Japanese order: family name first (e.g. "Oda Nobunaga").
        String fullName = clan + " " + given;

        game.newGame(fullName, clan);
        Samurai you = game.state.player;
        you.name = fullName;
        you.clanName = clan;
        you.swordsmanship = sword;
        you.generalship = general;
        you.stealth = stealth;
        game.screens.show(Game.MAP);
    }

    // --- small UI builders ------------------------------------------------

    private JComponent fieldRow(String label, JTextField field) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel l = UiKit.label(label, UiKit.BODY, Palette.INK);
        l.setPreferredSize(new Dimension(190, 26));
        l.setMaximumSize(new Dimension(190, 26));
        field.setFont(UiKit.BODY);
        field.setMaximumSize(new Dimension(220, 28));
        row.add(l);
        row.add(field);
        return row;
    }

    private JComponent skillRow(String label, final int which) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel l = UiKit.label(label, UiKit.BODY, Palette.INK);
        l.setPreferredSize(new Dimension(280, 28));
        l.setMaximumSize(new Dimension(280, 28));

        JButton minus = smallBtn("−");
        JButton plus  = smallBtn("+");
        JLabel val = which == 0 ? swordVal : which == 1 ? generalVal : stealthVal;
        val.setPreferredSize(new Dimension(34, 26));
        val.setMaximumSize(new Dimension(34, 26));
        minus.addActionListener(e -> adjust(which, -1));
        plus.addActionListener(e -> adjust(which, +1));

        row.add(l);
        row.add(minus);
        row.add(Box.createHorizontalStrut(6));
        row.add(val);
        row.add(Box.createHorizontalStrut(6));
        row.add(plus);
        return row;
    }

    private JButton smallBtn(String s) {
        JButton b = UiKit.button(s);
        b.setFont(UiKit.HEAD);
        b.setMargin(new Insets(0, 8, 0, 8));
        b.setMaximumSize(new Dimension(40, 30));
        b.setPreferredSize(new Dimension(40, 30));
        return b;
    }

    private JComponent left(JComponent c) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(c);
        row.add(Box.createHorizontalGlue());
        return row;
    }

    private static JLabel plainLabel() { return UiKit.label("", UiKit.BODY, Palette.INK); }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        UiKit.aa(g);
        UiKit.paperBackground(g, getWidth(), getHeight());

        g.setColor(Palette.INK);
        g.setFont(UiKit.TITLE);
        g.drawString("Take Up the Sword", 60, 90);

        g.setFont(UiKit.BODY);
        g.setColor(Palette.INK_SOFT);
        g.drawString("Age 15, freshly of age (gempuku) — head of a leading family in your clan.", 62, 122);

        // Starting-reputation preview panel (right side).
        int px = getWidth() - 300, py = 150;
        g.setColor(Palette.PANEL);
        g.fillRect(px, py, 250, 150);
        g.setColor(Palette.PANEL_LINE);
        g.drawRect(px, py, 250, 150);
        g.setColor(Palette.CINNABAR_DK);
        g.setFont(UiKit.HEAD);
        g.drawString("Starting Fortune", px + 16, py + 28);
        g.setFont(UiKit.BODY);
        g.setColor(Palette.INK);
        g.drawString("Honour:  " + START_HONOR, px + 16, py + 58);
        g.drawString("Power:   " + START_POWER, px + 16, py + 82);
        g.drawString("Koku:    " + START_KOKU, px + 16, py + 106);
        g.setFont(UiKit.SMALL);
        g.setColor(Palette.DIM);
        g.drawString("Rank: Samurai (gokenin)", px + 16, py + 132);
    }
}
