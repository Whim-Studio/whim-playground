package com.tiwas.mahjong.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.tiwas.mahjong.model.GameState;

/** Shows the live wall count plus round / turn / seat-wind information. */
public final class WallPanel extends JPanel {

    private final JLabel wallLabel = new JLabel();
    private final JLabel roundLabel = new JLabel();
    private final JLabel turnLabel = new JLabel();

    public WallPanel() {
        setLayout(new GridLayout(3, 1, 0, 2));
        setOpaque(false);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 200, 180)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        Font f = new Font("SansSerif", Font.BOLD, 13);
        wallLabel.setFont(f);
        roundLabel.setFont(f);
        turnLabel.setFont(f);
        wallLabel.setForeground(Color.WHITE);
        roundLabel.setForeground(Color.WHITE);
        turnLabel.setForeground(new Color(255, 235, 150));
        add(wallLabel);
        add(roundLabel);
        add(turnLabel);
    }

    public void update(GameState state) {
        wallLabel.setText("Wall: " + state.getWall().remaining() + " tiles left");
        roundLabel.setText("Hand " + state.getHandNumber() + "/16  ·  Round wind: "
                + state.getRoundWind().label());
        turnLabel.setText("Turn: " + state.getCurrentPlayer().getName()
                + " (" + state.getCurrentPlayer().getSeatWind().label() + ")");
    }
}
