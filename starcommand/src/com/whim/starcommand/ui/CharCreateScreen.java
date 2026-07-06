package com.whim.starcommand.ui;

import com.whim.starcommand.app.Game;
import com.whim.starcommand.app.Screen;
import com.whim.starcommand.model.Character;
import com.whim.starcommand.render.Palette;
import com.whim.starcommand.render.Starfield;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

/**
 * Character creation. Roll a crew of up to eight, choosing each member's role
 * (skill loadout) and name, then launch to the Starport.
 */
public class CharCreateScreen extends Screen {

    private final Starfield stars = new Starfield(900, 640, 200, 7L);
    private final DefaultListModel<Character> model = new DefaultListModel<Character>();
    private final JList<Character> list = new JList<Character>(model);
    private final JTextField nameField = new JTextField(14);
    private final JComboBox<String> roleBox = new JComboBox<String>(com.whim.starcommand.engine.Content.roles());
    private Character candidate;
    private final JPanel statPanel = new JPanel(new GridLayout(0, 2, 6, 2));
    private JLabel countLabel;

    public CharCreateScreen(Game game) {
        super(game);
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        add(header(), BorderLayout.NORTH);
        add(rollPanel(), BorderLayout.WEST);
        add(rosterPanel(), BorderLayout.CENTER);
        add(footer(), BorderLayout.SOUTH);

        Keys.bind(this, "R", new Runnable() { public void run() { reroll(); } });
        Keys.bind(this, "A", new Runnable() { public void run() { addCandidate(); } });
        Keys.bind(this, "ENTER", new Runnable() { public void run() { launch(); } });
    }

    private JPanel header() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(UiKit.label("RECRUIT YOUR CREW", UiKit.HEAD, Palette.ACCENT), BorderLayout.WEST);
        countLabel = UiKit.label("", UiKit.BODY, Palette.TEXT_DIM);
        p.add(countLabel, BorderLayout.EAST);
        return p;
    }

    private JPanel rollPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setPreferredSize(new Dimension(320, 400));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.setOpaque(false);
        row1.add(UiKit.label("Name:", UiKit.BODY, Palette.TEXT));
        row1.add(nameField);
        p.add(row1);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.setOpaque(false);
        row2.add(UiKit.label("Role:", UiKit.BODY, Palette.TEXT));
        row2.add(roleBox);
        p.add(row2);

        statPanel.setOpaque(false);
        statPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Palette.PANEL_LINE), "Rolled stats"));
        p.add(statPanel);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btns.setOpaque(false);
        JButton reroll = UiKit.button("Re-roll (R)");
        reroll.addActionListener(e -> reroll());
        JButton add = UiKit.button("Add (A)");
        add.addActionListener(e -> addCandidate());
        btns.add(reroll);
        btns.add(add);
        p.add(btns);
        return p;
    }

    private JPanel rosterPanel() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setOpaque(false);
        p.add(UiKit.label("Crew roster (max 8)", UiKit.BODY, Palette.TEXT), BorderLayout.NORTH);
        list.setFont(UiKit.MONO);
        JScrollPane sc = new JScrollPane(list);
        sc.setPreferredSize(new Dimension(320, 380));
        p.add(sc, BorderLayout.CENTER);
        JButton remove = UiKit.button("Remove selected");
        remove.addActionListener(e -> removeSelected());
        p.add(remove, BorderLayout.SOUTH);
        return p;
    }

    private JPanel footer() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        p.setOpaque(false);
        JButton back = UiKit.button("Back");
        back.addActionListener(e -> game.screens.show(Game.MENU));
        JButton launch = UiKit.button("Launch to Starport (Enter)");
        launch.addActionListener(e -> launch());
        p.add(back);
        p.add(launch);
        return p;
    }

    private void reroll() {
        String role = (String) roleBox.getSelectedItem();
        candidate = game.charGen.roll(nameField.getText(), role);
        showCandidate();
    }

    private void showCandidate() {
        statPanel.removeAll();
        if (candidate != null) {
            addStat("Strength", candidate.strength);
            addStat("Speed", candidate.speed);
            addStat("Accuracy", candidate.accuracy);
            addStat("Intellect", candidate.intellect);
            addStat("Leadership", candidate.leadership);
            addStat("Willpower", candidate.willpower);
            addStat("Hit Points", candidate.maxHp);
        }
        statPanel.revalidate();
        statPanel.repaint();
    }

    private void addStat(String name, int val) {
        statPanel.add(UiKit.label(name, UiKit.MONO, Palette.TEXT_DIM));
        statPanel.add(UiKit.label(String.valueOf(val), UiKit.MONO, Palette.TEXT));
    }

    private void addCandidate() {
        if (candidate == null) reroll();
        if (model.size() >= 8) {
            JOptionPane.showMessageDialog(this, "Crew is already full (8).");
            return;
        }
        model.addElement(candidate);
        candidate = null;
        nameField.setText("");
        statPanel.removeAll();
        statPanel.revalidate();
        statPanel.repaint();
        updateCount();
    }

    private void removeSelected() {
        int i = list.getSelectedIndex();
        if (i >= 0) { model.remove(i); updateCount(); }
    }

    private void launch() {
        if (model.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Recruit at least one crew member first.");
            return;
        }
        game.state.crew.clear();
        for (int i = 0; i < model.size(); i++) game.state.crew.add(model.get(i));
        game.screens.show(Game.STARPORT);
    }

    private void updateCount() {
        countLabel.setText("Recruited: " + model.size() + " / 8");
    }

    @Override
    public void onShow() {
        model.clear();
        // seed a suggested captain so the player can launch immediately
        candidate = game.charGen.roll("", "Pilot");
        nameField.setText(candidate.name);
        roleBox.setSelectedItem("Pilot");
        showCandidate();
        updateCount();
    }

    @Override
    public String name() { return Game.CREATE; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        stars.paint(g2, getWidth(), getHeight());
    }
}
