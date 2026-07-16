package com.whim.tippingpoint.ui;

import com.whim.tippingpoint.domain.GameMode;
import com.whim.tippingpoint.domain.Rules;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Modal new-game setup dialog: player count (2-4), per-player name and human/AI
 * flag, and game mode (COMPETITIVE / COOPERATIVE). Produces the argument lists
 * that {@code DefaultGameEngine.newGame} expects.
 */
final class NewGameDialog extends JDialog {

    private boolean confirmed = false;

    private final JSpinner countSpinner;
    private final JComboBox<String> modeBox;
    private final JTextField[] nameFields = new JTextField[Rules.MAX_PLAYERS];
    private final JCheckBox[] aiBoxes = new JCheckBox[Rules.MAX_PLAYERS];
    private final JLabel[] rowLabels = new JLabel[Rules.MAX_PLAYERS];

    NewGameDialog(Frame owner) {
        super(owner, "New Game — Tipping Point", true);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        root.setBackground(new Color(0x1e2733));

        JLabel title = new JLabel("Tipping Point");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(new Color(0x8fe388));
        JLabel subtitle = new JLabel("Grow your city — but keep global CO₂ below the tipping point.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(new Color(0xb8c2cc));
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.add(title);
        head.add(subtitle);
        root.add(head, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gc.gridx = 0; gc.gridy = row;
        form.add(label("Players:"), gc);
        countSpinner = new JSpinner(new SpinnerNumberModel(2, Rules.MIN_PLAYERS, Rules.MAX_PLAYERS, 1));
        gc.gridx = 1;
        form.add(countSpinner, gc);

        gc.gridx = 2;
        form.add(label("Mode:"), gc);
        modeBox = new JComboBox<String>(new String[] { "COMPETITIVE", "COOPERATIVE" });
        gc.gridx = 3;
        form.add(modeBox, gc);
        row++;

        String[] defaults = { "Alice", "Bob", "Carol", "Dave" };
        for (int i = 0; i < Rules.MAX_PLAYERS; i++) {
            gc.gridy = row + i;
            gc.gridx = 0;
            rowLabels[i] = label("Player " + (i + 1) + ":");
            form.add(rowLabels[i], gc);

            nameFields[i] = new JTextField(defaults[i], 12);
            gc.gridx = 1; gc.gridwidth = 2;
            form.add(nameFields[i], gc);
            gc.gridwidth = 1;

            aiBoxes[i] = new JCheckBox("AI");
            aiBoxes[i].setOpaque(false);
            aiBoxes[i].setForeground(new Color(0xb8c2cc));
            aiBoxes[i].setSelected(i >= 1); // player 1 human, rest AI by default
            gc.gridx = 3;
            form.add(aiBoxes[i], gc);
        }
        root.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton cancel = new JButton("Cancel");
        JButton start = new JButton("Start Game");
        buttons.add(cancel);
        buttons.add(start);
        root.add(buttons, BorderLayout.SOUTH);

        countSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                syncRows();
            }
        });
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });
        start.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                dispose();
            }
        });

        getRootPane().setDefaultButton(start);
        syncRows();
        setContentPane(root);
        setResizable(false);
        pack();
        setLocationRelativeTo(owner);
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(0xe6edf3));
        l.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return l;
    }

    private void syncRows() {
        int n = playerCount();
        for (int i = 0; i < Rules.MAX_PLAYERS; i++) {
            boolean active = i < n;
            nameFields[i].setEnabled(active);
            aiBoxes[i].setEnabled(active);
            rowLabels[i].setEnabled(active);
        }
    }

    private int playerCount() {
        return ((Integer) countSpinner.getValue()).intValue();
    }

    boolean isConfirmed() {
        return confirmed;
    }

    List<String> getNames() {
        int n = playerCount();
        List<String> names = new ArrayList<String>();
        for (int i = 0; i < n; i++) {
            String v = nameFields[i].getText().trim();
            if (v.isEmpty()) {
                v = "Player " + (i + 1);
            }
            names.add(v);
        }
        return names;
    }

    List<Boolean> getAiFlags() {
        int n = playerCount();
        List<Boolean> ai = new ArrayList<Boolean>();
        for (int i = 0; i < n; i++) {
            ai.add(Boolean.valueOf(aiBoxes[i].isSelected()));
        }
        return ai;
    }

    GameMode getMode() {
        return "COOPERATIVE".equals(modeBox.getSelectedItem())
                ? GameMode.COOPERATIVE : GameMode.COMPETITIVE;
    }

    /** Deterministic-per-launch seed for engine shuffles. */
    long getSeed() {
        return System.nanoTime();
    }

    // Keep a sensible minimum footprint.
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        if (d.width < 420) {
            d.width = 420;
        }
        return d;
    }
}
