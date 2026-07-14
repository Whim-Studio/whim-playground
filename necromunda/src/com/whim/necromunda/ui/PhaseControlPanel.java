package com.whim.necromunda.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.whim.necromunda.engine.GameState;
import com.whim.necromunda.engine.Phase;
import com.whim.necromunda.engine.TurnManager;

/**
 * The Milestone-4 turn control strip: a large current-phase indicator (active
 * gang + turn number + phase, with the five phases shown as a stepper) and the
 * {@code Next Phase} / {@code End Turn} buttons. Buttons drive the
 * {@link TurnManager}; the panel re-reads {@link GameState} on every change.
 */
public final class PhaseControlPanel extends JPanel {

    private final GameState state;
    private final TurnManager turns;

    private final JLabel activeLabel = new JLabel("", SwingConstants.CENTER);
    private final JPanel stepper = new JPanel(new GridLayout(1, Phase.values().length, 4, 0));
    private final JLabel[] stepLabels = new JLabel[Phase.values().length];
    private final JButton nextButton = new JButton("Next Phase ▶");
    private final JButton endTurnButton = new JButton("End Turn ⏭");

    public PhaseControlPanel(GameState state, TurnManager turns) {
        this.state = state;
        this.turns = turns;
        setLayout(new java.awt.BorderLayout(6, 6));
        setBackground(new Color(0x20, 0x20, 0x26));
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        activeLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        activeLabel.setForeground(Color.WHITE);
        add(activeLabel, java.awt.BorderLayout.NORTH);

        stepper.setOpaque(false);
        Phase[] phases = Phase.values();
        for (int i = 0; i < phases.length; i++) {
            JLabel l = new JLabel(phases[i].label(), SwingConstants.CENTER);
            l.setOpaque(true);
            l.setFont(new Font("SansSerif", Font.PLAIN, 11));
            l.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            stepLabels[i] = l;
            stepper.add(l);
        }
        add(stepper, java.awt.BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));
        buttons.setOpaque(false);
        nextButton.addActionListener(e -> {
            turns.advancePhase();
        });
        endTurnButton.addActionListener(e -> {
            turns.endTurn();
        });
        buttons.add(nextButton);
        buttons.add(endTurnButton);
        add(buttons, java.awt.BorderLayout.SOUTH);

        setPreferredSize(new Dimension(360, 120));
        refresh();
    }

    /** Re-read game state and update every widget. Call on each change. */
    public void refresh() {
        String active = state.activeGang().name();
        if (turns.isBattleOver()) {
            activeLabel.setText("Battle over — " + (turns.winner() == null ? "" : turns.winner().name() + " wins"));
            nextButton.setEnabled(false);
            endTurnButton.setEnabled(false);
        } else {
            activeLabel.setText("Turn " + state.turnNumber() + " — " + active
                    + "  •  " + state.phase().label() + " phase");
            nextButton.setEnabled(true);
            endTurnButton.setEnabled(true);
        }

        Phase current = state.phase();
        Phase[] phases = Phase.values();
        for (int i = 0; i < phases.length; i++) {
            boolean isCurrent = phases[i] == current;
            stepLabels[i].setBackground(isCurrent ? new Color(0xE0, 0xA0, 0x20)
                    : new Color(0x30, 0x30, 0x38));
            stepLabels[i].setForeground(isCurrent ? Color.BLACK : new Color(0xA0, 0xA0, 0xA8));
        }
        repaint();
    }
}
