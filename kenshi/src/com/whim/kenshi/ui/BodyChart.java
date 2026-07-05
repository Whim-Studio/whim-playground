package com.whim.kenshi.ui;

import com.whim.kenshi.api.Enums;
import com.whim.kenshi.api.Views;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

/**
 * Bottom-left status panel for the currently-selected character. Renders the
 * seven body parts in canonical {@link Enums.BodyPart} order (colour-graded by
 * remaining fraction, greyed when disabled), followed by Hunger and Blood bars
 * and a bleed indicator. Reads only {@link Views} — never a concrete class.
 */
public final class BodyChart extends JPanel {

    private final Font titleFont = new Font("SansSerif", Font.BOLD, 13);
    private final Font labelFont = new Font("SansSerif", Font.PLAIN, 11);

    private volatile Views.GameStateView state;

    public BodyChart() {
        setPreferredSize(new Dimension(236, 240));
        setBackground(Palette.HUD_PANEL);
    }

    public void setState(Views.GameStateView s) {
        this.state = s;
        repaint();
    }

    private Views.CharacterView selected() {
        Views.GameStateView s = state;
        if (s == null) return null;
        List<String> ids = s.selectedIds();
        if (ids == null || ids.isEmpty()) return null;
        String id = ids.get(0);
        List<Views.CharacterView> chars = s.characters();
        for (int i = 0; i < chars.size(); i++) {
            if (chars.get(i).id().equals(id)) return chars.get(i);
        }
        return null;
    }

    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(Palette.HUD_BORDER);
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

        Views.CharacterView ch = selected();
        int x = 12;
        int y = 22;
        g.setFont(titleFont);
        if (ch == null) {
            g.setColor(Palette.HUD_TEXT_DIM);
            g.drawString("No unit selected", x, y);
            g.setFont(labelFont);
            g.drawString("Left-click a squad member.", x, y + 20);
            return;
        }

        g.setColor(Palette.HUD_TEXT);
        g.drawString(ch.name(), x, y);
        g.setFont(labelFont);
        g.setColor(Palette.HUD_TEXT_DIM);
        g.drawString(stateText(ch), x, y + 15);

        int barX = x + 66;
        int barW = getWidth() - barX - 14;
        int rowH = 18;
        y += 30;

        Enums.BodyPart[] parts = Enums.BodyPart.values();
        for (int i = 0; i < parts.length; i++) {
            Enums.BodyPart part = parts[i];
            double max = ch.partMax(part);
            double hp = ch.partHp(part);
            double frac = max > 0 ? hp / max : 0;
            boolean disabled = ch.partDisabled(part);

            g.setFont(labelFont);
            g.setColor(disabled ? Palette.HP_OFF.brighter() : Palette.HUD_TEXT);
            g.drawString(part.label(), x, y + 10);

            drawBar(g, barX, y, barW, 11,
                    clamp01(frac),
                    disabled ? Palette.HP_OFF : Palette.grade(clamp01(frac)),
                    formatHp(hp, max));
            y += rowH;
        }

        y += 6;
        drawBar(g, barX, y, barW, 11, clamp01(ch.hunger() / ch.hungerMax()),
                Palette.HUNGER, null);
        g.setColor(Palette.HUD_TEXT);
        g.drawString("Hunger", x, y + 10);
        y += rowH;

        drawBar(g, barX, y, barW, 11, clamp01(ch.blood() / ch.bloodMax()),
                Palette.BLOOD, null);
        g.setColor(Palette.HUD_TEXT);
        g.drawString("Blood", x, y + 10);
        y += rowH + 4;

        if (ch.bleedRate() > 0.01) {
            g.setColor(Palette.BLEED);
            g.fillOval(x, y, 8, 8);
            g.drawString(String.format("Bleeding  -%.1f/s", ch.bleedRate()), x + 12, y + 8);
        } else {
            g.setColor(Palette.HUD_TEXT_DIM);
            g.drawString("Not bleeding", x, y + 8);
        }
    }

    private void drawBar(Graphics2D g, int x, int y, int w, int h,
                         double frac, Color fill, String overlay) {
        g.setColor(new Color(20, 18, 16));
        g.fillRect(x, y, w, h);
        g.setColor(fill);
        g.fillRect(x, y, (int) (w * frac), h);
        g.setColor(Palette.HUD_BORDER);
        g.drawRect(x, y, w, h);
        if (overlay != null) {
            g.setFont(labelFont);
            g.setColor(Palette.HUD_TEXT);
            int tw = g.getFontMetrics().stringWidth(overlay);
            g.drawString(overlay, x + w - tw - 3, y + h - 1);
        }
    }

    private static String stateText(Views.CharacterView ch) {
        String ms = ch.moveState().name();
        return ch.faction().label() + "  ·  " + ms;
    }

    private static String formatHp(double hp, double max) {
        return String.format("%d/%d", Math.round(hp), Math.round(max));
    }

    private static double clamp01(double v) {
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }
}
