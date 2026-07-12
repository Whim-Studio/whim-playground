package com.whim.firetop.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * New-game setup: pick the number of hot-seat adventurers (1-4) and name them.
 */
public final class SetupPanel extends JPanel {

    /** Called when the player confirms a new game. */
    public interface StartHandler {
        void onStart(List<String> names);
    }

    private final JComboBox<Integer> countBox = new JComboBox<Integer>(new Integer[] { 1, 2, 3, 4 });
    private final JTextField[] nameFields = new JTextField[4];
    private final StartHandler handler;

    public SetupPanel(StartHandler handler) {
        this.handler = handler;
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());

        JLabel title = new JLabel("The Warlock of Firetop Mountain", JLabel.CENTER);
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.GOLD);
        title.setBorder(BorderFactory.createEmptyBorder(24, 0, 4, 0));
        add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 8, 6, 8);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0; gc.gridy = 0;
        form.add(label("Number of adventurers:"), gc);
        gc.gridx = 1;
        countBox.setSelectedIndex(0);
        form.add(countBox, gc);

        String[] defaults = { "Sword", "Rowan", "Bram", "Ysolde" };
        for (int i = 0; i < 4; i++) {
            gc.gridx = 0; gc.gridy = i + 1;
            form.add(label("Adventurer " + (i + 1) + " name:"), gc);
            gc.gridx = 1;
            nameFields[i] = new JTextField(defaults[i], 14);
            form.add(nameFields[i], gc);
        }

        countBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { syncFields(); }
        });
        syncFields();

        add(form, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.setOpaque(false);
        JButton begin = new JButton("Enter the Mountain");
        begin.setMnemonic('E');
        begin.setBackground(Theme.EMERALD);
        begin.setForeground(Color.WHITE);
        begin.setFont(Theme.HEADING);
        begin.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { start(); }
        });
        south.add(begin);

        JLabel disclaimer = new JLabel("<html><center>Unofficial fan-made educational recreation. "
                + "Not affiliated with Games Workshop or the original creators.<br>"
                + "No original text or artwork is reproduced.</center></html>", JLabel.CENTER);
        disclaimer.setForeground(Theme.STONE_LIGHT);
        JPanel southWrap = new JPanel(new BorderLayout());
        southWrap.setOpaque(false);
        southWrap.add(south, BorderLayout.NORTH);
        southWrap.add(disclaimer, BorderLayout.SOUTH);
        southWrap.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));
        add(southWrap, BorderLayout.SOUTH);
    }

    private JLabel label(String s) {
        JLabel l = new JLabel(s);
        l.setForeground(Theme.PARCHMENT);
        l.setFont(Theme.BODY);
        return l;
    }

    private void syncFields() {
        int n = (Integer) countBox.getSelectedItem();
        for (int i = 0; i < 4; i++) {
            nameFields[i].setEnabled(i < n);
        }
    }

    private void start() {
        int n = (Integer) countBox.getSelectedItem();
        List<String> names = new ArrayList<String>();
        for (int i = 0; i < n; i++) {
            String nm = nameFields[i].getText().trim();
            names.add(nm.isEmpty() ? ("Adventurer " + (i + 1)) : nm);
        }
        handler.onStart(names);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(720, 520);
    }
}
