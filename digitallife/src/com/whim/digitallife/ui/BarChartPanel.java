package com.whim.digitallife.ui;

import com.whim.digitallife.model.ResultProfile;
import com.whim.digitallife.model.Trait;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * A self-contained, hand-drawn horizontal bar chart of the five personality
 * traits, rendered with {@link Graphics2D} — no external chart libraries.
 *
 * <p>Each trait is drawn as a rounded track with a filled bar sized to its
 * normalized percentage, colored with the trait's theme color.</p>
 */
public final class BarChartPanel extends JPanel {

    private ResultProfile profile;

    /** Creates an empty chart panel; call {@link #setProfile} to populate it. */
    public BarChartPanel() {
        setBackground(Theme.SURFACE);
        setPreferredSize(new Dimension(520, 260));
    }

    /**
     * Sets the profile to visualize and repaints.
     *
     * @param profile the results to chart (may be null to clear)
     */
    public void setProfile(ResultProfile profile) {
        this.profile = profile;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (profile == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Trait[] traits = Trait.values();
            int padding = 18;
            int labelWidth = 150;
            int rowGap = 14;
            int chartWidth = getWidth() - padding * 2 - labelWidth - 50;
            int usableHeight = getHeight() - padding * 2;
            int rowHeight = (usableHeight - rowGap * (traits.length - 1)) / traits.length;
            int barHeight = Math.min(rowHeight, 26);

            Font labelFont = Theme.SMALL_FONT.deriveFont(Font.BOLD);
            g2.setFont(labelFont);

            int y = padding;
            for (Trait trait : traits) {
                int percent = profile.getPercent(trait);
                int barX = padding + labelWidth;
                int centerY = y + rowHeight / 2;
                int barY = centerY - barHeight / 2;

                // Trait label (right-aligned against the bar start).
                g2.setColor(Theme.TEXT);
                String name = trait.getDisplayName();
                int textWidth = g2.getFontMetrics().stringWidth(name);
                g2.drawString(name, barX - textWidth - 12,
                        centerY + g2.getFontMetrics().getAscent() / 2 - 2);

                // Background track.
                g2.setColor(new Color(0x2C, 0x30, 0x44));
                g2.fillRoundRect(barX, barY, chartWidth, barHeight, barHeight, barHeight);

                // Filled portion.
                int fillWidth = (int) Math.round(chartWidth * (percent / 100.0));
                fillWidth = Math.max(fillWidth, percent > 0 ? barHeight : 0);
                g2.setColor(trait.getColor());
                g2.fillRoundRect(barX, barY, fillWidth, barHeight, barHeight, barHeight);

                // Percentage label at the end of the row.
                g2.setColor(Theme.TEXT_MUTED);
                g2.drawString(percent + "%", barX + chartWidth + 10,
                        centerY + g2.getFontMetrics().getAscent() / 2 - 2);

                y += rowHeight + rowGap;
            }
        } finally {
            g2.dispose();
        }
    }
}
