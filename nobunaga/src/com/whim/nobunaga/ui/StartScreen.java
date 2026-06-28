package com.whim.nobunaga.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Daimyo selection screen. Shows the eight fixed roster warlords, each with a
 * procedurally drawn flag swatch (no external images), and lets the player pick
 * one. On selection it hands the chosen daimyo id back to its
 * {@link SelectionHandler} and disposes itself; {@code Main} builds the game.
 */
public final class StartScreen extends JFrame {

    /** Callback fired once with the chosen daimyo id (0..7). */
    public interface SelectionHandler {
        void onDaimyoChosen(int daimyoId);
    }

    /** Display-only copy of the FIXED roster (id, name, abbrev, color, home). */
    private static final String[] NAMES = {
            "Oda Nobunaga", "Takeda Shingen", "Uesugi Kenshin", "Hojo Ujiyasu",
            "Mori Motonari", "Shimazu Yoshihisa", "Date Terumune", "Chosokabe Kunichika"
    };
    private static final String[] ABBREV = {
            "ODA", "TAK", "UES", "HOJ", "MOR", "SHI", "DAT", "CHO"
    };
    private static final String[] HOME = {
            "Owari", "Kai", "Echigo", "Sagami", "Aki", "Satsuma", "Mutsu-S", "Tosa"
    };
    private static final Color[] COLORS = {
            new Color(200, 40, 40), new Color(40, 80, 200), new Color(70, 160, 210),
            new Color(225, 140, 30), new Color(40, 150, 70), new Color(170, 60, 170),
            new Color(120, 90, 160), new Color(210, 190, 40)
    };

    private final SelectionHandler handler;

    public StartScreen(SelectionHandler handler) {
        super("Nobunaga's Ambition: Zenkokuban");
        this.handler = handler;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(20, 24, 30));

        JLabel title = new JLabel("Choose your Daimyo", JLabel.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 28));
        title.setForeground(new Color(235, 225, 200));
        title.setBorder(BorderFactory.createEmptyBorder(18, 0, 4, 0));
        add(title, BorderLayout.NORTH);

        JLabel sub = new JLabel("Sengoku, 1560 — unify the realm under one banner", JLabel.CENTER);
        sub.setFont(new Font("Serif", Font.ITALIC, 15));
        sub.setForeground(new Color(170, 175, 180));

        JPanel grid = new JPanel(new GridLayout(2, 4, 14, 14));
        grid.setBackground(new Color(20, 24, 30));
        grid.setBorder(BorderFactory.createEmptyBorder(16, 20, 20, 20));
        for (int i = 0; i < NAMES.length; i++) {
            grid.add(card(i));
        }

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(new Color(20, 24, 30));
        center.add(sub, BorderLayout.NORTH);
        center.add(grid, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        pack();
        setMinimumSize(new Dimension(820, 520));
        setLocationRelativeTo(null);
    }

    private JPanel card(final int id) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(new Color(32, 36, 44));
        panel.setBorder(BorderFactory.createLineBorder(COLORS[id].darker(), 2));

        FlagSwatch flag = new FlagSwatch(id);
        panel.add(flag, BorderLayout.CENTER);

        JLabel label = new JLabel("<html><center>" + NAMES[id]
                + "<br><font size='-2'>" + HOME[id] + "</font></center></html>", JLabel.CENTER);
        label.setForeground(new Color(225, 225, 225));
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panel.add(label, BorderLayout.NORTH);

        JButton choose = new JButton("Choose " + ABBREV[id]);
        choose.setFocusable(false);
        choose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
                handler.onDaimyoChosen(id);
            }
        });
        panel.add(choose, BorderLayout.SOUTH);
        return panel;
    }

    /** A small procedurally-drawn clan flag (banner + mon + abbreviation). */
    private static final class FlagSwatch extends JPanel {
        private final int id;

        FlagSwatch(int id) {
            this.id = id;
            setPreferredSize(new Dimension(150, 96));
            setBackground(new Color(32, 36, 44));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            Color base = COLORS[id];

            // Banner cloth.
            g2.setColor(base.darker());
            g2.fillRect(12, 8, w - 24, h - 16);
            g2.setColor(base);
            g2.fillRect(16, 12, w - 32, h - 24);

            // Mon (white disc) with the clan abbreviation.
            int cx = w / 2;
            int cy = h / 2;
            int d = Math.min(w, h) - 40;
            g2.setColor(new Color(245, 245, 240));
            g2.fillOval(cx - d / 2, cy - d / 2, d, d);
            g2.setColor(base.darker());
            g2.drawOval(cx - d / 2, cy - d / 2, d, d);

            g2.setColor(new Color(30, 30, 34));
            g2.setFont(new Font("Monospaced", Font.BOLD, 16));
            String a = ABBREV[id];
            int sw = g2.getFontMetrics().stringWidth(a);
            g2.drawString(a, cx - sw / 2, cy + 6);
            g2.dispose();
        }
    }
}
