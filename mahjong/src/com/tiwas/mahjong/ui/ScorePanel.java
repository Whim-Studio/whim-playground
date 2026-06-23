package com.tiwas.mahjong.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.tiwas.mahjong.model.GameState;
import com.tiwas.mahjong.model.Player;

/** Shows each player's seat wind, score, and dealer / current-turn markers. */
public final class ScorePanel extends JPanel {

    private final JLabel[] rows = new JLabel[4];

    public ScorePanel() {
        setLayout(new GridLayout(5, 1, 0, 4));
        setOpaque(false);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(200, 200, 200)), "Scores"),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        JLabel header = new JLabel("Player   Seat   Score");
        header.setFont(new Font("Monospaced", Font.BOLD, 12));
        header.setForeground(new Color(230, 230, 230));
        add(header);
        for (int i = 0; i < 4; i++) {
            rows[i] = new JLabel();
            rows[i].setFont(new Font("Monospaced", Font.PLAIN, 13));
            rows[i].setForeground(Color.WHITE);
            add(rows[i]);
        }
    }

    public void update(GameState state) {
        for (int i = 0; i < state.getPlayers().size(); i++) {
            Player p = state.getPlayer(i);
            String marker = "";
            if (i == state.getDealer()) {
                marker += "D";
            }
            if (i == state.getCurrentTurn()) {
                marker += ">";
            }
            String name = pad(p.getName(), 7);
            String seat = pad(p.getSeatWind().label(), 6);
            rows[i].setText(marker.isEmpty() ? "  " : pad(marker, 2));
            rows[i].setText(pad(marker, 2) + name + seat + p.getScore());
            rows[i].setForeground(i == state.getCurrentTurn()
                    ? new Color(255, 235, 150) : Color.WHITE);
        }
    }

    private String pad(String s, int n) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
