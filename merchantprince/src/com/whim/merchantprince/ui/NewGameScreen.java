package com.whim.merchantprince.ui;

import com.whim.merchantprince.app.Game;
import com.whim.merchantprince.app.Screen;
import com.whim.merchantprince.render.Palette;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Character/house creation: pick a family surname, crest colour and game length,
 * then start (GAME_DESIGN_REFERENCE §8 — you choose a crest and surname, not a name).
 */
public class NewGameScreen extends Screen {

    private final JTextField surname = new JTextField("Polo", 16);
    private final JComboBox<String> crest = new JComboBox<String>(
            new String[] { "Gold", "Crimson", "Azure", "Emerald" });
    private final JComboBox<String> length = new JComboBox<String>(
            new String[] { "Short (30 years)", "Standard (60 years)", "Long (100 years)" });

    private static final Color[] CREST_COLORS = {
            Palette.GOLD, Palette.CRIMSON, new Color(0x2E5A88), Palette.GREEN };
    private static final int[] END_YEARS = { 1330, 1360, 1400 };

    public NewGameScreen(Game game) {
        super(game);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        length.setSelectedIndex(1);

        add(Box.createVerticalStrut(40));
        addRow(UiKit.label("Found Your House", UiKit.TITLE, Palette.INK));
        add(Box.createVerticalStrut(30));
        addRow(UiKit.label("Family surname:", UiKit.HEAD, Palette.INK));
        addRow(surname);
        add(Box.createVerticalStrut(14));
        addRow(UiKit.label("Crest colour:", UiKit.HEAD, Palette.INK));
        addRow(crest);
        add(Box.createVerticalStrut(14));
        addRow(UiKit.label("Game length:", UiKit.HEAD, Palette.INK));
        addRow(length);
        add(Box.createVerticalStrut(30));

        JButton start = UiKit.button("Set Sail");
        start.addActionListener(e -> {
            String name = surname.getText().trim();
            if (name.isEmpty()) name = "Polo";
            int crestColor = CREST_COLORS[crest.getSelectedIndex()].getRGB();
            int endYear = END_YEARS[length.getSelectedIndex()];
            game.newGame(name, crestColor, endYear);
            game.screens.show(Game.MAP);
        });
        addRow(start);
        add(Box.createVerticalStrut(10));
        JButton back = UiKit.button("Back");
        back.addActionListener(e -> game.screens.show(Game.MENU));
        addRow(back);
        add(Box.createVerticalGlue());
    }

    private void addRow(JComponent c) {
        c.setAlignmentX(CENTER_ALIGNMENT);
        c.setMaximumSize(new Dimension(360, 40));
        add(c);
    }

    @Override public String name() { return Game.NEWGAME; }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setPaint(Palette.PARCHMENT);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    @Override public Dimension getPreferredSize() { return new Dimension(900, 680); }
}
