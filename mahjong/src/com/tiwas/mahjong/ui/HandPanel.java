package com.tiwas.mahjong.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.tiwas.mahjong.engine.HandAnalyzer;
import com.tiwas.mahjong.model.Hand;
import com.tiwas.mahjong.model.Tile;

/**
 * Renders a player's concealed tiles. For the human it shows face-up,
 * clickable tiles; for the AI seats it shows tile backs.
 */
public final class HandPanel extends JPanel {

    /** Notified when the human clicks a tile (to discard it). */
    public interface TileSelectListener {
        void tileSelected(Tile tile);
    }

    private final boolean faceUp;
    private TileSelectListener listener;
    private boolean selectable;

    public HandPanel(boolean faceUp) {
        this.faceUp = faceUp;
        setLayout(new FlowLayout(FlowLayout.LEFT, 3, 3));
        setOpaque(false);
    }

    public void setTileSelectListener(TileSelectListener l) {
        this.listener = l;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

    public void update(Hand hand) {
        removeAll();
        if (faceUp) {
            List<Tile> sorted = HandAnalyzer.sorted(hand.getTiles());
            for (int i = 0; i < sorted.size(); i++) {
                final Tile t = sorted.get(i);
                TileView tv = new TileView(t, true);
                tv.setEnabled(selectable);
                tv.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (selectable && listener != null) {
                            listener.tileSelected(t);
                        }
                    }
                });
                add(tv);
            }
        } else {
            int n = hand.getTiles().size();
            for (int i = 0; i < n; i++) {
                add(back());
            }
        }
        revalidate();
        repaint();
    }

    private JLabel back() {
        JLabel l = new JLabel("▮", JLabel.CENTER);
        l.setOpaque(true);
        l.setBackground(new Color(60, 110, 70));
        l.setForeground(new Color(30, 70, 40));
        l.setFont(new Font("SansSerif", Font.BOLD, 28));
        l.setPreferredSize(new Dimension(26, 60));
        l.setBorder(javax.swing.BorderFactory.createLineBorder(new Color(20, 50, 30), 1));
        return l;
    }
}
