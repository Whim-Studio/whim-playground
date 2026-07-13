package com.whim.babylon5.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;

import com.whim.babylon5.data.ImageLoader;
import com.whim.babylon5.domain.Card;
import com.whim.babylon5.domain.ConflictType;

/**
 * A single tabletop card. Renders the (cached/async) art from {@link ImageLoader}
 * over a faction-tinted frame, plus the four conflict attributes, cost, and the
 * ready / damage in-play state. Selecting toggles a highlight and notifies a listener.
 */
final class CardView extends JComponent {

    interface SelectionListener {
        void onCardClicked(CardView view, Card card);
    }

    static final int W = 116;
    static final int H = 162;

    private final Card card;
    private boolean selected;
    private SelectionListener listener;

    CardView(Card card) {
        this.card = card;
        setPreferredSize(new Dimension(W, H));
        setToolTipText(buildTooltip());
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (listener != null) {
                    listener.onCardClicked(CardView.this, CardView.this.card);
                }
            }
        });
    }

    Card card() { return card; }

    void setSelectionListener(SelectionListener l) { this.listener = l; }

    void setSelected(boolean s) {
        if (s != selected) {
            selected = s;
            repaint();
        }
    }

    boolean isSelected() { return selected; }

    private String buildTooltip() {
        StringBuilder sb = new StringBuilder("<html><b>").append(esc(card.getName()))
                .append("</b><br>").append(card.getType()).append(" — ")
                .append(card.getFaction()).append("<br>Cost ").append(card.getCost())
                .append("<br>D ").append(card.getDiplomacy())
                .append(" / I ").append(card.getIntrigue())
                .append(" / P ").append(card.getPsi())
                .append(" / M ").append(card.getMilitary());
        if (card.getText() != null && !card.getText().isEmpty()) {
            sb.append("<br><i>").append(esc(card.getText())).append("</i>");
        }
        if (!card.getAttachments().isEmpty()) {
            sb.append("<br><b>Enhancements:</b>");
            for (Card e : card.getAttachments()) {
                sb.append("<br>• ").append(esc(e.getName()))
                  .append(" (D").append(e.getDiplomacy()).append("/I").append(e.getIntrigue())
                  .append("/P").append(e.getPsi()).append("/M").append(e.getMilitary()).append(")");
            }
        }
        return sb.append("</html>").toString();
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        Color frame = UiTheme.factionColor(card.getFaction());
        // frame
        g2.setColor(UiTheme.PANEL_HI);
        g2.fillRoundRect(0, 0, W - 1, H - 1, 12, 12);
        g2.setColor(frame.darker());
        g2.fillRoundRect(3, 3, W - 7, H - 7, 10, 10);

        // art window: real art if a host ever serves it, otherwise a
        // faction-tinted panel showing the card type (the shipped card data
        // carries no working art URLs, so this is the normal case).
        int ax = 8, ay = 22, aw = W - 16, ah = 92;
        Image art = ImageLoader.getReal(card.getImageUrl());
        if (art != null) {
            g2.drawImage(art, ax, ay, aw, ah, null);
        } else {
            paintArtFallback(g2, ax, ay, aw, ah, frame);
        }
        g2.setColor(new Color(0, 0, 0, 90));
        g2.drawRect(ax, ay, aw, ah);

        // title bar
        g2.setColor(frame);
        g2.setFont(UiTheme.H2.deriveFont(11f));
        g2.drawString(fit(g2, card.getName(), W - 26), 8, 16);

        // cost chip
        g2.setColor(UiTheme.GOLD);
        g2.fillOval(W - 22, 4, 16, 16);
        g2.setColor(UiTheme.SPACE);
        g2.setFont(UiTheme.H2.deriveFont(11f));
        g2.drawString(String.valueOf(card.getCost()), W - 18, 16);

        // attribute strip D / I / P / M
        int sy = ay + ah + 14;
        drawAttr(g2, 10,        sy, "D", card.getDiplomacy(), ConflictType.DIPLOMACY);
        drawAttr(g2, 10 + 27,   sy, "I", card.getIntrigue(),  ConflictType.INTRIGUE);
        drawAttr(g2, 10 + 54,   sy, "P", card.getPsi(),       ConflictType.PSI);
        drawAttr(g2, 10 + 81,   sy, "M", card.getMilitary(),  ConflictType.MILITARY);

        // type label
        g2.setColor(UiTheme.INK_DIM);
        g2.setFont(UiTheme.BODY.deriveFont(9f));
        g2.drawString(card.getType().toString(), 8, H - 8);

        // attachment badge (enhancements pinned to this card)
        int attached = card.getAttachments().size();
        if (attached > 0) {
            g2.setColor(UiTheme.OK);
            g2.fillRoundRect(W - 26, H - 20, 20, 14, 8, 8);
            g2.setColor(UiTheme.SPACE);
            g2.setFont(UiTheme.MONO.deriveFont(10f));
            g2.drawString("+" + attached, W - 23, H - 9);
        }

        // in-play state: marked (exhausted) overlay + damage badge
        if (!card.isReady()) {
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRoundRect(3, 3, W - 7, H - 7, 10, 10);
            g2.setColor(UiTheme.INK_DIM);
            g2.setFont(UiTheme.BODY.deriveFont(10f));
            g2.drawString("MARKED", 8, H / 2);
        }
        if (card.getDamage() > 0) {
            g2.setColor(UiTheme.DANGER);
            g2.fillOval(6, H - 24, 18, 18);
            g2.setColor(UiTheme.INK);
            g2.drawString(String.valueOf(card.getDamage()), 11, H - 11);
        }

        // selection ring
        if (selected) {
            g2.setColor(UiTheme.ACCENT);
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(2, 2, W - 5, H - 5, 12, 12);
        }
        g2.dispose();
    }

    /** A faction-tinted art panel used when no real card art is available. */
    private void paintArtFallback(Graphics2D g2, int ax, int ay, int aw, int ah, Color frame) {
        java.awt.Paint old = g2.getPaint();
        g2.setPaint(new java.awt.GradientPaint(
                ax, ay, frame.darker().darker(),
                ax, ay + ah, frame.darker()));
        g2.fillRect(ax, ay, aw, ah);
        g2.setPaint(old);
        // faint faction initial as a crest
        String crest = card.getFaction().toString();
        crest = crest.isEmpty() ? "?" : crest.substring(0, 1);
        g2.setColor(new Color(255, 255, 255, 40));
        g2.setFont(UiTheme.H1.deriveFont(46f));
        int cw = g2.getFontMetrics().stringWidth(crest);
        g2.drawString(crest, ax + (aw - cw) / 2, ay + ah / 2 + 16);
        // card type caption
        g2.setColor(new Color(235, 240, 255, 200));
        g2.setFont(UiTheme.BODY.deriveFont(9f));
        String ty = card.getType().toString();
        int tw = g2.getFontMetrics().stringWidth(ty);
        g2.drawString(ty, ax + (aw - tw) / 2, ay + ah - 6);
    }

    private void drawAttr(Graphics2D g2, int x, int y, String key, int val, ConflictType t) {
        g2.setColor(UiTheme.conflictColor(t));
        g2.setFont(UiTheme.MONO.deriveFont(10f));
        g2.drawString(key, x, y);
        g2.setColor(UiTheme.INK);
        g2.drawString(String.valueOf(val), x + 10, y);
    }

    private static String fit(Graphics2D g2, String s, int max) {
        if (g2.getFontMetrics().stringWidth(s) <= max) {
            return s;
        }
        String dots = "…";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (g2.getFontMetrics().stringWidth(sb.toString() + s.charAt(i) + dots) > max) {
                break;
            }
            sb.append(s.charAt(i));
        }
        return sb + dots;
    }
}
