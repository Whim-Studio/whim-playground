package com.whim.starcommand.ui;

import com.whim.starcommand.render.Palette;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Font;

/** Consistent Swing styling helpers so every screen shares one visual language. */
public final class UiKit {
    private UiKit() { }

    public static final Font TITLE = new Font("Monospaced", Font.BOLD, 34);
    public static final Font HEAD  = new Font("Monospaced", Font.BOLD, 18);
    public static final Font BODY  = new Font("Monospaced", Font.PLAIN, 14);
    public static final Font MONO  = new Font("Monospaced", Font.PLAIN, 13);

    public static JButton button(String text) {
        JButton b = new JButton(text);
        b.setFont(HEAD);
        b.setForeground(Palette.TEXT);
        b.setBackground(Palette.PANEL);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Palette.ACCENT, 1),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)));
        b.setOpaque(true);
        return b;
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    public static void transparent(JComponent c) {
        c.setOpaque(false);
    }
}
