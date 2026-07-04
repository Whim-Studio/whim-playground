package com.whim.swd6.ui;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * Small shared UI helpers: styled buttons, section labels, a starfield background
 * panel, and common GridBag constraints. Keeps the individual panels terse.
 *
 * Owned by Task 3 (ui).
 */
public final class Ui {

    private Ui() {
    }

    /** A flat accent button in the space theme. */
    public static JButton button(String text) {
        JButton b = new JButton(text);
        style(b, Palette.AMBER, Palette.SPACE_DEEP);
        return b;
    }

    /** A secondary (cyan) button. */
    public static JButton ghost(String text) {
        JButton b = new JButton(text);
        style(b, Palette.SPACE_RAISED, Palette.TEXT);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Palette.CYAN, 1),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        return b;
    }

    private static void style(AbstractButton b, Color bg, Color fg) {
        b.setFocusPainted(false);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(Palette.HEAD);
        b.setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
        b.setOpaque(true);
        b.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Palette.TITLE);
        l.setForeground(Palette.AMBER);
        return l;
    }

    public static JLabel head(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Palette.HEAD);
        l.setForeground(Palette.CYAN);
        return l;
    }

    public static JLabel body(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Palette.BODY);
        l.setForeground(Palette.TEXT);
        return l;
    }

    public static JLabel dim(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Palette.SMALL);
        l.setForeground(Palette.TEXT_DIM);
        return l;
    }

    /** A read-only, word-wrapping log/text area styled for the theme. */
    public static JTextArea logArea() {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBackground(Palette.SPACE_DEEP);
        a.setForeground(Palette.TEXT);
        a.setFont(Palette.BODY);
        a.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        return a;
    }

    public static JScrollPane scroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createLineBorder(Palette.GRID_LINE, 1));
        sp.getViewport().setBackground(Palette.SPACE_DEEP);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    public static GridBagConstraints gbc(int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        return c;
    }

    /** A panel that paints a subtle deep-space starfield gradient background. */
    public static class SpaceBackground extends JPanel {
        private final int[] starX;
        private final int[] starY;
        private final int[] starS;

        public SpaceBackground() {
            setOpaque(true);
            // deterministic star layout (no Math.random needed; keeps it stable)
            int n = 90;
            starX = new int[n];
            starY = new int[n];
            starS = new int[n];
            long seed = 0x5DEECE66DL;
            for (int i = 0; i < n; i++) {
                seed = (seed * 25214903917L + 11) & ((1L << 48) - 1);
                starX[i] = (int) ((seed >> 16) % 1600);
                seed = (seed * 25214903917L + 11) & ((1L << 48) - 1);
                starY[i] = (int) ((seed >> 16) % 1000);
                seed = (seed * 25214903917L + 11) & ((1L << 48) - 1);
                starS[i] = 1 + (int) ((seed >> 20) % 2);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setPaint(new java.awt.GradientPaint(0, 0, Palette.SPACE_DEEP, 0, h,
                    Palette.blend(Palette.SPACE_DEEP, Palette.SPACE_PANEL, 0.6f)));
            g2.fillRect(0, 0, w, h);
            for (int i = 0; i < starX.length; i++) {
                int x = starX[i] % Math.max(1, w);
                int y = starY[i] % Math.max(1, h);
                g2.setColor(i % 7 == 0 ? Palette.CYAN : Palette.STAR_DIM);
                g2.fillOval(x, y, starS[i], starS[i]);
            }
            g2.dispose();
        }
    }

    public static Dimension dim(int w, int h) {
        return new Dimension(w, h);
    }
}
