package com.whim.tarot.ui;

import com.whim.tarot.domain.DrawnCard;
import com.whim.tarot.engine.PositionedCard;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a single positioned Tarot card. Shows a text placeholder (card name)
 * until its RWS image is supplied via {@link #setImage}, at which point it
 * repaints with the artwork. Reversed cards are drawn rotated 180 degrees and
 * carry a "Reversed" badge; the Celtic Cross "crossing" card is drawn rotated
 * 90 degrees (its component is sized in landscape so the rotated card fits).
 */
final class CardView extends JComponent {

    static final int CARD_W = 104;
    static final int CARD_H = 168;

    private static final Color PARCHMENT = new Color(0xF4, 0xEC, 0xD8);
    private static final Color BORDER = new Color(0x3A, 0x2E, 0x1A);
    private static final Color SELECTED = new Color(0xC8, 0x9B, 0x3C);
    private static final Color TEXT = new Color(0x2A, 0x22, 0x12);

    private final PositionedCard positioned;
    private final boolean crossing;
    private final boolean reversed;

    private BufferedImage image; // null until loaded
    private boolean selected;

    CardView(PositionedCard positioned, boolean crossing) {
        this.positioned = positioned;
        this.crossing = crossing;
        DrawnCard dc = positioned.getDrawnCard();
        this.reversed = dc.isReversed();
        Dimension d = crossing
                ? new Dimension(CARD_H, CARD_W)
                : new Dimension(CARD_W, CARD_H);
        setPreferredSize(d);
        setSize(d);
    }

    PositionedCard getPositioned() {
        return positioned;
    }

    void setImage(BufferedImage img) {
        this.image = img;
        repaint();
    }

    void setSelected(boolean sel) {
        if (this.selected != sel) {
            this.selected = sel;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        double rot = 0.0;
        if (crossing) {
            rot += Math.PI / 2.0;
        }
        if (reversed) {
            rot += Math.PI;
        }
        g2.translate(getWidth() / 2.0, getHeight() / 2.0);
        if (rot != 0.0) {
            g2.rotate(rot);
        }
        drawCardBody(g2);
        g2.dispose();

        // Non-rotated overlay: a "Reversed" badge so orientation is unambiguous
        // even when the artwork is symmetric.
        if (reversed) {
            Graphics2D gb = (Graphics2D) g.create();
            gb.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gb.setFont(getFont().deriveFont(Font.BOLD, 10f));
            String badge = "REVERSED";
            FontMetrics fm = gb.getFontMetrics();
            int bw = fm.stringWidth(badge) + 8;
            int bh = fm.getHeight() + 2;
            int bx = (getWidth() - bw) / 2;
            int by = getHeight() - bh - 2;
            gb.setColor(new Color(0x8B, 0x1A, 0x1A));
            gb.fillRoundRect(bx, by, bw, bh, 6, 6);
            gb.setColor(Color.WHITE);
            gb.drawString(badge, bx + 4, by + fm.getAscent());
            gb.dispose();
        }
    }

    /** Draws the card centred at the graphics origin in portrait logical space. */
    private void drawCardBody(Graphics2D g2) {
        int w = CARD_W;
        int h = CARD_H;
        int x = -w / 2;
        int y = -h / 2;

        RoundRectangle2D.Float frame = new RoundRectangle2D.Float(x, y, w, h, 12, 12);
        g2.setColor(PARCHMENT);
        g2.fill(frame);

        if (image != null) {
            int pad = 4;
            g2.drawImage(image, x + pad, y + pad, w - 2 * pad, h - 2 * pad, null);
        } else {
            drawPlaceholderText(g2, x, y, w, h);
        }

        g2.setColor(selected ? SELECTED : BORDER);
        g2.setStroke(new BasicStroke(selected ? 3.5f : 1.5f));
        g2.draw(frame);
    }

    private void drawPlaceholderText(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(TEXT);
        g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
        String name = positioned.getDrawnCard().getCard().getName();
        List<String> lines = wrap(name, g2.getFontMetrics(), w - 12);
        FontMetrics fm = g2.getFontMetrics();
        int totalH = lines.size() * fm.getHeight();
        int ty = -totalH / 2 + fm.getAscent();
        for (int i = 0; i < lines.size(); i++) {
            String ln = lines.get(i);
            int lw = fm.stringWidth(ln);
            g2.drawString(ln, -lw / 2, ty + i * fm.getHeight());
        }
    }

    private static List<String> wrap(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<String>();
        String[] words = text.split("\\s+");
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String w = words[i];
            String trial = cur.length() == 0 ? w : cur + " " + w;
            if (fm.stringWidth(trial) > maxWidth && cur.length() > 0) {
                lines.add(cur.toString());
                cur = new StringBuilder(w);
            } else {
                cur = new StringBuilder(trial);
            }
        }
        if (cur.length() > 0) {
            lines.add(cur.toString());
        }
        return lines;
    }
}
