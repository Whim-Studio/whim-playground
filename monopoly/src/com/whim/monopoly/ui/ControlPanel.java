package com.whim.monopoly.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.whim.monopoly.domain.Player;
import com.whim.monopoly.engine.GameEngine;
import com.whim.monopoly.engine.GameState;
import com.whim.monopoly.engine.TurnPhase;

/**
 * Primary turn controls. Buttons are enabled/disabled purely from
 * {@link GameState#getPhase()} and the current player's jail state; all actions
 * go through the {@link GameEngine}.
 */
public class ControlPanel extends JPanel {

    private final MonopolyFrame frame;
    private final GameEngine engine;

    private final JLabel turnLabel = new JLabel();
    private final JLabel diceLabel = new JLabel();
    private final JButton rollBtn = new JButton("Roll Dice");
    private final JButton buyBtn = new JButton("Buy");
    private final JButton declineBtn = new JButton("Decline → Auction");
    private final JButton endBtn = new JButton("End Turn");
    private final JButton payJailBtn = new JButton("Pay $50");
    private final JButton useCardBtn = new JButton("Use Jail Card");
    private final JButton bankruptBtn = new JButton("Declare Bankruptcy");

    public ControlPanel(MonopolyFrame frame, GameEngine engine) {
        this.frame = frame;
        this.engine = engine;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder("Turn"));

        turnLabel.setFont(turnLabel.getFont().deriveFont(Font.BOLD, 14f));
        turnLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        diceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(turnLabel);
        add(diceLabel);
        add(Box.createVerticalStrut(6));

        JPanel grid = new JPanel(new GridLayout(0, 2, 6, 6));
        grid.add(rollBtn);
        grid.add(endBtn);
        grid.add(buyBtn);
        grid.add(declineBtn);
        grid.add(payJailBtn);
        grid.add(useCardBtn);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(grid);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        bankruptBtn.setForeground(new Color(150, 30, 30));
        bottom.add(bankruptBtn);
        bottom.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(bottom);

        rollBtn.addActionListener(run(new Runnable() {
            public void run() {
                engine.rollDice();
            }
        }));
        buyBtn.addActionListener(run(new Runnable() {
            public void run() {
                engine.buyProperty();
            }
        }));
        declineBtn.addActionListener(run(new Runnable() {
            public void run() {
                engine.declineProperty();
            }
        }));
        endBtn.addActionListener(run(new Runnable() {
            public void run() {
                engine.endTurn();
            }
        }));
        payJailBtn.addActionListener(run(new Runnable() {
            public void run() {
                engine.payJailFine();
            }
        }));
        useCardBtn.addActionListener(run(new Runnable() {
            public void run() {
                engine.useJailCard();
            }
        }));
        bankruptBtn.addActionListener(run(new Runnable() {
            public void run() {
                engine.declareBankruptcy();
            }
        }));

        Dimension max = new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        setMaximumSize(max);
    }

    private ActionListener run(final Runnable action) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.submitEngineAction(action);
            }
        };
    }

    /** Recompute button enablement from live state. Must run on the EDT. */
    public void refresh() {
        GameState s = engine.getState();
        TurnPhase phase = s.getPhase();
        Player cur = s.getCurrentPlayer();
        boolean inJail = cur != null && cur.isInJail();

        if (cur != null) {
            turnLabel.setText(cur.getName() + "  —  $" + cur.getCash());
            turnLabel.setForeground(cur.getToken() != null ? cur.getToken().darker() : Color.BLACK);
        } else {
            turnLabel.setText("—");
        }
        int[] dice = s.getLastDice();
        if (dice != null && (dice[0] != 0 || dice[1] != 0)) {
            String dbl = dice[0] == dice[1] ? "  (doubles)" : "";
            diceLabel.setText("Dice: " + dice[0] + " + " + dice[1] + " = " + (dice[0] + dice[1]) + dbl);
        } else {
            diceLabel.setText("Dice: —");
        }

        boolean awaitRoll = phase == TurnPhase.AWAITING_ROLL;
        rollBtn.setEnabled(awaitRoll);
        buyBtn.setEnabled(phase == TurnPhase.AWAITING_BUY);
        declineBtn.setEnabled(phase == TurnPhase.AWAITING_BUY);
        endBtn.setEnabled(phase == TurnPhase.AWAITING_END_TURN);
        payJailBtn.setEnabled(awaitRoll && inJail && cur.getCash() >= 50);
        useCardBtn.setEnabled(awaitRoll && inJail && cur.getJailCards() > 0);
        bankruptBtn.setEnabled(phase != TurnPhase.GAME_OVER && phase != TurnPhase.AUCTION);
    }
}
