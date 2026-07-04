package com.whim.emotiv;

import javax.swing.*;
import java.awt.*;

/** A labeled 0..1 horizontal bar. Value/label set on the EDT only. */
public class AffectivGauge extends JPanel {

    private final String label;
    private final Color fill;
    private float value = 0f;
    private String dynLabel;

    public AffectivGauge(String label, Color fill) {
        this.label = label;
        this.fill = fill;
        setPreferredSize(new Dimension(260, 34));
    }

    public void setValue(float v) {
        this.value = (v < 0f) ? 0f : (v > 1f ? 1f : v);
        repaint();
    }

    public void setLabel(String text) {
        this.dynLabel = text;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);

        int pad = 6, barH = 14;
        int barY = getHeight() - barH - pad;
        int barW = getWidth() - 2 * pad;

        String text = (dynLabel != null ? dynLabel : label);
        g.setColor(Color.GRAY);
        g.drawString(text + "  " + String.format("%.2f", value), pad, pad + 10);

        g.setColor(new Color(230, 230, 230));
        g.fillRoundRect(pad, barY, barW, barH, 8, 8);
        g.setColor(fill);
        g.fillRoundRect(pad, barY, Math.round(barW * value), barH, 8, 8);
        g.setColor(Color.LIGHT_GRAY);
        g.drawRoundRect(pad, barY, barW, barH, 8, 8);
    }
}
