package com.whim.nobunaga.ui;

import com.whim.nobunaga.domain.Daimyo;
import com.whim.nobunaga.domain.GameState;
import com.whim.nobunaga.domain.Province;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * CENTER component: a procedural Sengoku map of all 50 provinces, drawn with
 * {@link Graphics2D} only (no external images).
 *
 * <p>Each province lives in a virtual {@code 0..1000} coordinate space and is
 * scaled into the panel. Provinces are filled rounded rectangles tinted by their
 * owning {@link Daimyo}'s color (gray when neutral); adjacency edges are drawn
 * underneath; a clan marker (colored square + abbreviation) and the soldier count
 * label each province. Clicking a province selects it (visual highlight) and
 * notifies a {@link SelectionListener}.
 */
public final class MapPanel extends JPanel {

    /** Listener notified when the user clicks a province. */
    public interface SelectionListener {
        void provinceSelected(int provinceId);
    }

    private static final int VIRTUAL = 1000;
    private static final int MARGIN = 28;
    private static final int BOX_W = 50;
    private static final int BOX_H = 38;

    private static final Color BACKGROUND = new Color(18, 26, 34);
    private static final Color SEA_LINE = new Color(40, 60, 78);
    private static final Color ADJ_LINE = new Color(70, 92, 110);
    private static final Color NEUTRAL = new Color(110, 110, 116);
    private static final Color HIGHLIGHT = new Color(250, 240, 150);

    private final GameState state;
    private SelectionListener listener;

    /** Screen rectangles for the current paint, used for click hit-testing. */
    private final Map<Integer, Rectangle> hitBoxes = new HashMap<Integer, Rectangle>();

    private int selected = -1;

    public MapPanel(GameState state) {
        this.state = state;
        setPreferredSize(new Dimension(820, 760));
        setBackground(BACKGROUND);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        });
    }

    public void setSelectionListener(SelectionListener listener) {
        this.listener = listener;
    }

    public void setSelected(int provinceId) {
        this.selected = provinceId;
    }

    private void handleClick(int mx, int my) {
        for (Map.Entry<Integer, Rectangle> entry : hitBoxes.entrySet()) {
            if (entry.getValue().contains(mx, my)) {
                int id = entry.getKey().intValue();
                this.selected = id;
                if (listener != null) {
                    listener.provinceSelected(id);
                }
                repaint();
                return;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        double scaleX = (getWidth() - 2.0 * MARGIN) / VIRTUAL;
        double scaleY = (getHeight() - 2.0 * MARGIN) / VIRTUAL;

        hitBoxes.clear();

        // First pass: adjacency edges (drawn under the province boxes).
        g2.setColor(ADJ_LINE);
        Stroke old = g2.getStroke();
        g2.setStroke(new BasicStroke(1.4f));
        for (Province p : state.provinces) {
            int x1 = sx(p.getX(), scaleX);
            int y1 = sy(p.getY(), scaleY);
            for (Integer adj : p.getAdjacent()) {
                if (adj.intValue() <= p.getId()) {
                    continue; // draw each edge once
                }
                Province q = state.province(adj.intValue());
                g2.drawLine(x1, y1, sx(q.getX(), scaleX), sy(q.getY(), scaleY));
            }
        }
        g2.setStroke(old);

        // Second pass: province boxes + markers.
        for (Province p : state.provinces) {
            drawProvince(g2, p, scaleX, scaleY);
        }

        g2.dispose();
    }

    private void drawProvince(Graphics2D g2, Province p, double scaleX, double scaleY) {
        int cx = sx(p.getX(), scaleX);
        int cy = sy(p.getY(), scaleY);
        int x = cx - BOX_W / 2;
        int y = cy - BOX_H / 2;
        hitBoxes.put(Integer.valueOf(p.getId()), new Rectangle(x, y, BOX_W, BOX_H));

        Color base = ownerColor(p);
        g2.setColor(base.darker());
        g2.fillRoundRect(x, y, BOX_W, BOX_H, 12, 12);
        g2.setColor(base);
        g2.fillRoundRect(x + 2, y + 2, BOX_W - 4, BOX_H - 4, 10, 10);

        // Selection highlight.
        if (p.getId() == selected) {
            Stroke old = g2.getStroke();
            g2.setStroke(new BasicStroke(2.6f));
            g2.setColor(HIGHLIGHT);
            g2.drawRoundRect(x - 2, y - 2, BOX_W + 4, BOX_H + 4, 14, 14);
            g2.setStroke(old);
        }

        // Province name (tiny, top).
        g2.setColor(textOn(base));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
        drawCentered(g2, p.getName(), cx, y + 11);

        // Clan marker: colored square + abbrev.
        String abbrev = "—";
        Color markColor = NEUTRAL.darker();
        if (!p.isNeutral()) {
            Daimyo d = state.daimyo(p.getOwnerId());
            abbrev = d.getAbbrev();
            markColor = d.getColor();
        }
        int ms = 12;
        int mx = x + 4;
        int myy = cy - ms / 2;
        g2.setColor(markColor.darker());
        g2.fillRect(mx, myy, ms, ms);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 9));
        g2.drawString(abbrev, mx + ms + 2, cy + 3);

        // Soldier count (bottom).
        g2.setColor(textOn(base));
        g2.setFont(new Font("Monospaced", Font.BOLD, 9));
        drawCentered(g2, String.valueOf(p.getSoldiers()), cx, y + BOX_H - 3);
    }

    private Color ownerColor(Province p) {
        if (p.isNeutral()) {
            return NEUTRAL;
        }
        return state.daimyo(p.getOwnerId()).getColor();
    }

    private static Color textOn(Color bg) {
        int luma = (bg.getRed() * 299 + bg.getGreen() * 587 + bg.getBlue() * 114) / 1000;
        return luma > 140 ? new Color(20, 20, 24) : Color.WHITE;
    }

    private int sx(int virtualX, double scaleX) {
        return MARGIN + (int) Math.round(virtualX * scaleX);
    }

    private int sy(int virtualY, double scaleY) {
        return MARGIN + (int) Math.round(virtualY * scaleY);
    }

    private void drawCentered(Graphics2D g2, String text, int cx, int baselineY) {
        int w = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, cx - w / 2, baselineY);
    }
}
