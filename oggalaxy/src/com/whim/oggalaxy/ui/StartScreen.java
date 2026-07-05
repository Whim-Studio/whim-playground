package com.whim.oggalaxy.ui;

import com.whim.oggalaxy.api.ClassDef;
import com.whim.oggalaxy.api.GameController;
import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.api.NewGameSetup;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 * New-game setup screen: commander name, opponent count (1–7) with a per-opponent
 * difficulty (and optional name), and a player-class chooser. On "Start Game" it
 * validates, builds a {@link NewGameSetup}, calls {@link GameController#newGame} and
 * hands off to {@link MainFrame}.
 */
public final class StartScreen extends JFrame {

    private final GameController controller;

    private final JTextField nameField = new JTextField("Commander", 16);
    private final JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 7, 1));
    private final JComboBox<Ids.PlayerClass> classBox = new JComboBox<Ids.PlayerClass>(Ids.PlayerClass.values());
    private final JTextField seedField = new JTextField("42", 8);
    private final JPanel opponentsPanel = new JPanel();
    private final List<JComboBox<Ids.Difficulty>> diffBoxes = new ArrayList<JComboBox<Ids.Difficulty>>();
    private final List<JTextField> oppNameFields = new ArrayList<JTextField>();
    private final JLabel classDesc = UiUtil.label("", Palette.TEXT_DIM, Palette.FONT_SMALL);

    public StartScreen(GameController controller) {
        super("OG Galaxy — New Game");
        this.controller = controller;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(620, 660);
        setLocationRelativeTo(null);

        StarPanel bg = new StarPanel(1337L);
        bg.setLayout(new BorderLayout());
        bg.setBorder(UiUtil.padded(20, 40, 20, 40));

        JPanel card = new JPanel();
        card.setOpaque(true);
        card.setBackground(Palette.alpha(Palette.BG_PANEL, 235));
        card.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(Palette.BORDER_HI, 1),
                UiUtil.padded(18, 24, 18, 24)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = UiUtil.label("OG  GALAXY", Palette.ACCENT, Palette.FONT_BIG);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitle = UiUtil.label("Command an empire against AI rivals", Palette.TEXT_DIM, Palette.FONT);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(16));

        card.add(row("Commander name", nameField));
        card.add(Box.createVerticalStrut(8));

        JPanel classRow = row("Player class", classBox);
        card.add(classRow);
        classDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(classDesc);
        classBox.addActionListener(e -> updateClassDesc());
        UiUtil.themeDark(classBox);
        updateClassDesc();
        card.add(Box.createVerticalStrut(8));

        JPanel countRow = row("Opponents (1–7)", countSpinner);
        card.add(countRow);
        countSpinner.addChangeListener(e -> rebuildOpponents());
        card.add(Box.createVerticalStrut(6));

        opponentsPanel.setOpaque(false);
        opponentsPanel.setLayout(new BoxLayout(opponentsPanel, BoxLayout.Y_AXIS));
        opponentsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JScrollPane oppScroll = new JScrollPane(opponentsPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        oppScroll.setBorder(UiUtil.panelBorder("Opponents"));
        oppScroll.getViewport().setBackground(Palette.BG_PANEL);
        oppScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        oppScroll.setPreferredSize(new Dimension(520, 220));
        oppScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        card.add(oppScroll);
        card.add(Box.createVerticalStrut(8));

        card.add(row("Seed", seedField));
        card.add(Box.createVerticalStrut(14));

        JButton start = UiUtil.button("Start Game", Palette.OK);
        start.setFont(Palette.FONT_TITLE);
        start.setAlignmentX(Component.CENTER_ALIGNMENT);
        start.addActionListener(e -> startGame());
        card.add(start);

        bg.add(card, BorderLayout.CENTER);
        setContentPane(bg);
        rebuildOpponents();
    }

    private JPanel row(String labelText, Component field) {
        JPanel r = new JPanel(new BorderLayout(10, 0));
        r.setOpaque(false);
        r.setAlignmentX(Component.LEFT_ALIGNMENT);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        JLabel l = UiUtil.label(labelText, Palette.TEXT, Palette.FONT_BOLD);
        l.setPreferredSize(new Dimension(150, 26));
        r.add(l, BorderLayout.WEST);
        if (field instanceof javax.swing.JComponent) {
            styleInput((javax.swing.JComponent) field);
        }
        r.add(field, BorderLayout.CENTER);
        return r;
    }

    private void styleInput(javax.swing.JComponent c) {
        c.setBackground(Palette.BG_PANEL_HI);
        c.setForeground(Palette.TEXT);
        if (c instanceof JTextField) {
            ((JTextField) c).setCaretColor(Palette.ACCENT);
            c.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(Palette.BORDER, 1),
                    UiUtil.padded(3, 6, 3, 6)));
        }
    }

    private void updateClassDesc() {
        Ids.PlayerClass pc = (Ids.PlayerClass) classBox.getSelectedItem();
        ClassDef def = controller.catalog().playerClass(pc);
        classDesc.setText(def == null ? "" : "   " + def.description);
    }

    private void rebuildOpponents() {
        int count = ((Number) countSpinner.getValue()).intValue();
        opponentsPanel.removeAll();
        diffBoxes.clear();
        oppNameFields.clear();
        String[] defaultNames = {"Zarkon Hegemony", "Vega Collective", "Orion Syndicate",
                "Draco Imperium", "Nyx Concord", "Rigel Dominion", "Kael Ascendancy"};
        for (int i = 0; i < count; i++) {
            JPanel r = new JPanel(new BorderLayout(8, 0));
            r.setOpaque(false);
            r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            r.setBorder(UiUtil.padded(2, 4, 2, 4));

            JLabel idx = UiUtil.label("#" + (i + 1), Palette.ACCENT, Palette.FONT_BOLD);
            idx.setPreferredSize(new Dimension(32, 24));
            r.add(idx, BorderLayout.WEST);

            JTextField nf = new JTextField(defaultNames[i % defaultNames.length]);
            styleInput(nf);
            r.add(nf, BorderLayout.CENTER);
            oppNameFields.add(nf);

            JComboBox<Ids.Difficulty> db = new JComboBox<Ids.Difficulty>(Ids.Difficulty.values());
            db.setSelectedItem(i == 0 ? Ids.Difficulty.MEDIUM : Ids.Difficulty.RANDOM);
            UiUtil.themeDark(db);
            db.setPreferredSize(new Dimension(110, 24));
            diffBoxes.add(db);
            r.add(db, BorderLayout.EAST);

            opponentsPanel.add(r);
        }
        opponentsPanel.revalidate();
        opponentsPanel.repaint();
    }

    private void startGame() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty()) {
            error("Please enter a commander name.");
            return;
        }
        int count = ((Number) countSpinner.getValue()).intValue();
        if (count < 1 || count > 7) {
            error("Opponent count must be between 1 and 7.");
            return;
        }
        long seed;
        try {
            String s = seedField.getText().trim();
            seed = s.isEmpty() ? mixSeed(name) : Long.parseLong(s);
        } catch (NumberFormatException ex) {
            seed = mixSeed(name);
        }

        List<NewGameSetup.AIConfig> opponents = new ArrayList<NewGameSetup.AIConfig>();
        for (int i = 0; i < count; i++) {
            String oppName = i < oppNameFields.size() ? oppNameFields.get(i).getText().trim() : null;
            if (oppName != null && oppName.isEmpty()) oppName = null;
            Ids.Difficulty d = i < diffBoxes.size()
                    ? (Ids.Difficulty) diffBoxes.get(i).getSelectedItem() : Ids.Difficulty.MEDIUM;
            opponents.add(new NewGameSetup.AIConfig(oppName, d));
        }
        Ids.PlayerClass pc = (Ids.PlayerClass) classBox.getSelectedItem();

        NewGameSetup setup = new NewGameSetup(name, pc, opponents, seed);
        controller.newGame(setup);

        MainFrame frame = new MainFrame(controller);
        frame.launch();
        dispose();
    }

    private static long mixSeed(String name) {
        long h = 1125899906842597L;
        for (int i = 0; i < name.length(); i++) h = 31 * h + name.charAt(i);
        return h;
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Cannot start", JOptionPane.WARNING_MESSAGE);
    }
}
