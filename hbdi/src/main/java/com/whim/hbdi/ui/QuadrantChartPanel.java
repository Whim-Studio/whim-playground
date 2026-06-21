package com.whim.hbdi.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import com.whim.hbdi.domain.Quadrant;
import com.whim.hbdi.domain.QuadrantScore;

/**
 * Custom four-quadrant HBDI profile chart drawn entirely with Graphics2D.
 * No external chart libraries. Each quadrant is rendered as a wedge whose
 * radius is proportional to that quadrant's percentage, with the percentage
 * and label drawn over it.
 *
 * Layout follows the classic Whole Brain model:
 *   D (Imaginative, upper-left)   |   A (Analytical, upper-right)
 *   ------------------------------+------------------------------
 *   C (Interpersonal, lower-left) |   B (Sequential, lower-right)
 */
public class QuadrantChartPanel extends JPanel {

    private final Map<Quadrant, Double> percentages = new EnumMap<Quadrant, Double>(Quadrant.class);

    private static final Color COLOR_A = new Color(0x2E, 0x86, 0xC1); // blue
    private static final Color COLOR_B = new Color(0x27, 0xAE, 0x60); // green
    private static final Color COLOR_C = new Color(0xE6, 0x7E, 0x22); // orange
    private static final Color COLOR_D = new Color(0x8E, 0x44, 0xAD); // purple

    public QuadrantChartPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(440, 440));
        for (Quadrant q : Quadrant.values()) {
            percentages.put(q, 0.0);
        }
    }

    /** Update the chart from the scoring engine's output and repaint. */
    public void setScores(List<QuadrantScore> scores) {
        for (Quadrant q : Quadrant.values()) {
            percentages.put(q, 0.0);
        }
        if (scores != null) {
            for (QuadrantScore s : scores) {
                if (s != null && s.getQuadrant() != null) {
                    percentages.put(s.getQuadrant(), s.getPercentage());
                }
            }
        }
        repaint();
    }

    private static Color colorFor(Quadrant q) {
        switch (q) {
            case A: return COLOR_A;
            case B: return COLOR_B;
            case C: return COLOR_C;
            case D: return COLOR_D;
            default: return Color.GRAY;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int cx = w / 2;
            int cy = h / 2;
            int margin = 56;
            int maxRadius = Math.max(40, Math.min(w, h) / 2 - margin);

            double maxPct = 0.0;
            for (Quadrant q : Quadrant.values()) {
                maxPct = Math.max(maxPct, percentages.get(q));
            }
            if (maxPct <= 0.0) {
                maxPct = 1.0;
            }

            // Reference gridlines (concentric quarter guides at 25/50/75/100% of max wedge).
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(0xE0, 0xE0, 0xE0));
            for (int i = 1; i <= 4; i++) {
                int r = maxRadius * i / 4;
                g2.drawOval(cx - r, cy - r, r * 2, r * 2);
            }

            // Each quadrant occupies a 90-degree wedge. Java arc angles are
            // counter-clockwise with 0 at 3 o'clock.
            //   A upper-right:   0..90
            //   D upper-left:   90..180
            //   C lower-left:  180..270
            //   B lower-right: 270..360
            drawWedge(g2, cx, cy, maxRadius, maxPct, Quadrant.A, 0);
            drawWedge(g2, cx, cy, maxRadius, maxPct, Quadrant.D, 90);
            drawWedge(g2, cx, cy, maxRadius, maxPct, Quadrant.C, 180);
            drawWedge(g2, cx, cy, maxRadius, maxPct, Quadrant.B, 270);

            // Axes through the centre.
            g2.setColor(new Color(0x9E, 0x9E, 0x9E));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(cx - maxRadius, cy, cx + maxRadius, cy);
            g2.drawLine(cx, cy - maxRadius, cx, cy + maxRadius);

            // Quadrant labels + percentages placed in each corner.
            int off = maxRadius / 2;
            drawLabel(g2, Quadrant.A, cx + off, cy - off);
            drawLabel(g2, Quadrant.D, cx - off, cy - off);
            drawLabel(g2, Quadrant.C, cx - off, cy + off);
            drawLabel(g2, Quadrant.B, cx + off, cy + off);
        } finally {
            g2.dispose();
        }
    }

    private void drawWedge(Graphics2D g2, int cx, int cy, int maxRadius,
                           double maxPct, Quadrant q, int startAngle) {
        double pct = percentages.get(q);
        int r = (int) Math.round(maxRadius * (pct / maxPct));
        if (r < 2) {
            return;
        }
        Color base = colorFor(q);
        g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 150));
        g2.fillArc(cx - r, cy - r, r * 2, r * 2, startAngle, 90);
        g2.setColor(base);
        g2.setStroke(new BasicStroke(2f));
        g2.drawArc(cx - r, cy - r, r * 2, r * 2, startAngle, 90);
    }

    private void drawLabel(Graphics2D g2, Quadrant q, int x, int y) {
        double pct = percentages.get(q);
        String pctText = formatPct(pct) + "%";
        String name = q.name() + " — " + q.getLabel();

        Font pctFont = getFont().deriveFont(Font.BOLD, 22f);
        Font nameFont = getFont().deriveFont(Font.PLAIN, 13f);

        g2.setColor(colorFor(q).darker());

        g2.setFont(pctFont);
        FontMetrics pm = g2.getFontMetrics();
        int pw = pm.stringWidth(pctText);
        g2.drawString(pctText, x - pw / 2, y);

        g2.setFont(nameFont);
        FontMetrics nm = g2.getFontMetrics();
        int nw = nm.stringWidth(name);
        g2.setColor(new Color(0x33, 0x33, 0x33));
        g2.drawString(name, x - nw / 2, y + 18);
    }

    private static String formatPct(double pct) {
        return String.format("%.1f", pct);
    }
}
