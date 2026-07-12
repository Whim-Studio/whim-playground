package com.whim.capes.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A styled "coming in a later phase" placeholder used by views whose full
 * implementation lands in Phase 2/3/5. It renders the view title and a note so
 * the shell is navigable and reviewable now, without pretending functionality
 * exists.
 */
public final class PlaceholderView extends JPanel {
    public PlaceholderView(String title, String note) {
        setLayout(new BorderLayout());
        setBackground(Palette.PAPER);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(BorderFactory.createEmptyBorder(60, 60, 60, 60));

        JLabel t = new JLabel(title);
        t.setFont(Palette.TITLE);
        t.setForeground(Palette.INK);
        t.setAlignmentX(LEFT_ALIGNMENT);

        JLabel n = new JLabel("<html><body style='width:520px'>" + note + "</body></html>");
        n.setFont(Palette.BODY);
        n.setForeground(Palette.MUTED);
        n.setAlignmentX(LEFT_ALIGNMENT);

        center.add(t);
        center.add(javax.swing.Box.createVerticalStrut(12));
        center.add(n);
        add(center, BorderLayout.CENTER);
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        UiKit.antialias(g2);
        // faint decorative dice strip along the bottom to hint the visual language
        int size = 34, y = getHeight() - size - 24, x = 60;
        Color[] accents = { Palette.HERO_BLUE, Palette.VILLAIN_RED, Palette.GOLD, Palette.DEBT, Palette.CONTROL };
        for (int i = 0; i < 5; i++) {
            UiKit.die(g2, x + i * (size + 12), y, size, i + 1, accents[i]);
        }
    }
}
