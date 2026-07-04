package com.whim.emotiv;

import javax.swing.*;
import java.awt.*;

/** Stacked live traces for N channels with per-channel ring buffers. EDT-only. */
public class WaveformPanel extends JPanel {

    private final String[] names;
    private final double[][] ring;   // [ch][cap]
    private final int cap;
    private int head = 0, filled = 0;
    private double gain = 0.05;      // px per µV (adjustable)

    public WaveformPanel(String[] names, int seconds) {
        this.names = names;
        this.cap = seconds * EdkConstants.RAW_SAMPLE_HZ;
        this.ring = new double[names.length][cap];
        setPreferredSize(new Dimension(560, 24 * names.length + 20));
        setBackground(Color.WHITE);
    }

    public void setGain(double g) { this.gain = g; repaint(); }

    /** Append a batch: samples[ch][i]. */
    public void append(double[][] samples) {
        if (samples.length == 0 || samples[0] == null) return;
        int n = samples[0].length;
        for (int i = 0; i < n; i++) {
            for (int c = 0; c < names.length && c < samples.length; c++) {
                ring[c][head] = samples[c][i];
            }
            head = (head + 1) % cap;
            if (filled < cap) filled++;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int rows = names.length;
        int rowH = (getHeight() - 10) / rows;
        int w = getWidth();

        for (int c = 0; c < rows; c++) {
            int baseY = 10 + c * rowH + rowH / 2;
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(0, baseY, w, baseY);
            g.setColor(Color.GRAY);
            g.drawString(names[c], 2, baseY - rowH / 2 + 10);

            if (filled < 2) continue;
            double mean = 0;                       // subtract DC offset per window
            for (int i = 0; i < filled; i++) mean += ring[c][(head - filled + i + cap) % cap];
            mean /= filled;

            g.setColor(new Color(40, 90, 160));
            int prevX = -1, prevY = 0;
            for (int i = 0; i < filled; i++) {
                double v = ring[c][(head - filled + i + cap) % cap] - mean;
                int x = (int) ((double) i / cap * w);
                int y = baseY - (int) (v * gain);
                if (y < baseY - rowH / 2) y = baseY - rowH / 2;
                if (y > baseY + rowH / 2) y = baseY + rowH / 2;
                if (prevX >= 0) g.drawLine(prevX, prevY, x, y);
                prevX = x; prevY = y;
            }
        }
    }
}
