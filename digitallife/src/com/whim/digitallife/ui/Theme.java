package com.whim.digitallife.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;

/**
 * Central place for the app's colors, fonts, and small styled-widget factories.
 *
 * <p>Keeping visual tokens here means the individual screens stay focused on
 * layout, and the whole app can be re-themed from one file.</p>
 */
public final class Theme {

    /** Deep background used behind every screen. */
    public static final Color BG = new Color(0x15, 0x17, 0x22);
    /** Slightly lighter surface for cards and panels. */
    public static final Color SURFACE = new Color(0x1F, 0x22, 0x33);
    /** Primary accent (used for main call-to-action buttons). */
    public static final Color ACCENT = new Color(0x6C, 0x5C, 0xE7);
    /** Softer secondary accent. */
    public static final Color ACCENT_SOFT = new Color(0x00, 0xB8, 0x94);
    /** Primary text color. */
    public static final Color TEXT = new Color(0xF1, 0xF2, 0xF6);
    /** Muted text for hints and secondary labels. */
    public static final Color TEXT_MUTED = new Color(0x9A, 0x9F, 0xB5);

    public static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 34);
    public static final Font HEADING_FONT = new Font("SansSerif", Font.BOLD, 22);
    public static final Font BODY_FONT = new Font("SansSerif", Font.PLAIN, 16);
    public static final Font SMALL_FONT = new Font("SansSerif", Font.PLAIN, 13);

    private Theme() {
        // Utility class; not instantiable.
    }

    /**
     * Builds a filled, rounded-feel primary button in the app's accent color.
     *
     * @param text the button label
     * @return a styled {@link JButton}
     */
    public static JButton primaryButton(String text) {
        return styledButton(text, ACCENT, Color.WHITE);
    }

    /**
     * Builds a secondary button (used for Back / neutral actions).
     *
     * @param text the button label
     * @return a styled {@link JButton}
     */
    public static JButton secondaryButton(String text) {
        return styledButton(text, SURFACE, TEXT);
    }

    private static JButton styledButton(String text, Color fill, Color fg) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(fill);
        button.setForeground(fg);
        button.setFont(BODY_FONT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
        // Opaque + no content-area-fill overlap keeps the flat color on all L&Fs.
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        return button;
    }

    /**
     * Applies a transparent background to a component so parent theming shows
     * through. Small convenience used across screens.
     *
     * @param component the component to make transparent
     */
    public static void makeTransparent(JComponent component) {
        component.setOpaque(false);
    }
}
