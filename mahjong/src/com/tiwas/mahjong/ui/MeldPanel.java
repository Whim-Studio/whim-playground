package com.tiwas.mahjong.ui;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.tiwas.mahjong.model.Meld;
import com.tiwas.mahjong.model.Tile;

/**
 * Shows a player's exposed/declared melds and their revealed bonus tiles
 * (flowers and seasons).
 */
public final class MeldPanel extends JPanel {

    public MeldPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 4, 1));
        setOpaque(false);
    }

    public void update(List<Meld> melds, List<Tile> bonus) {
        removeAll();
        for (int i = 0; i < melds.size(); i++) {
            Meld m = melds.get(i);
            JLabel l = new JLabel(label(m));
            l.setFont(new Font("Monospaced", Font.BOLD, 13));
            l.setForeground(m.isConcealed() ? new Color(110, 110, 110) : new Color(40, 40, 40));
            l.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4));
            add(l);
        }
        if (!bonus.isEmpty()) {
            StringBuilder sb = new StringBuilder("Bonus: ");
            for (int i = 0; i < bonus.size(); i++) {
                if (i > 0) {
                    sb.append(' ');
                }
                sb.append(bonus.get(i).code());
            }
            JLabel b = new JLabel(sb.toString());
            b.setFont(new Font("Monospaced", Font.PLAIN, 12));
            b.setForeground(new Color(200, 120, 0));
            add(b);
        }
        revalidate();
        repaint();
    }

    private String label(Meld m) {
        String kind = m.isKong() ? "Kong" : m.isChow() ? "Chow" : "Pung";
        return (m.isConcealed() ? "[" : "") + kind + " " + m.representative().code()
                + (m.isConcealed() ? "]" : "");
    }
}
