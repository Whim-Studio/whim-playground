package com.whim.cardwoven.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.whim.cardwoven.api.Enums.CardType;
import com.whim.cardwoven.api.GameController;
import com.whim.cardwoven.api.Views.CardView;
import com.whim.cardwoven.api.Views.GameStateView;
import com.whim.cardwoven.api.Views.PlayerView;

/**
 * Renders the current human player's hand as procedurally-drawn cards (no
 * images) and supports click-to-select. Selecting a card then clicking a tile
 * on the {@link MapPanel} plays it — the "click-and-drop" flow is coordinated by
 * {@link GameFrame}. The selected card lifts and glows.
 */
public class HandPanel extends JPanel {

    public interface CardSelectListener {
        void onCardSelected(int cardId);
    }

    private final GameController controller;
    private CardSelectListener listener;
    private int selectedCardId = -1;

    private final List<Rectangle> cardBounds = new ArrayList<Rectangle>();
    private final List<Integer> cardIds = new ArrayList<Integer>();
    private int hoverIndex = -1;

    public HandPanel(GameController controller) {
        this.controller = controller;
        setBackground(UiColors.PANEL_BG);
        setPreferredSize(new Dimension(900, 180));
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { handleClick(e.getPoint()); }
            @Override public void mouseMoved(MouseEvent e) { updateHover(e.getPoint()); }
            @Override public void mouseExited(MouseEvent e) {
                if (hoverIndex != -1) { hoverIndex = -1; repaint(); }
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    public void setCardSelectListener(CardSelectListener l) { this.listener = l; }

    public int selectedCardId() { return selectedCardId; }

    public void clearSelection() {
        if (selectedCardId != -1) {
            selectedCardId = -1;
            repaint();
        }
    }

    private void updateHover(Point p) {
        int idx = indexAt(p);
        if (idx != hoverIndex) { hoverIndex = idx; repaint(); }
    }

    private int indexAt(Point p) {
        // Iterate from the top-most (last drawn) card down.
        for (int i = cardBounds.size() - 1; i >= 0; i--) {
            if (cardBounds.get(i).contains(p)) return i;
        }
        return -1;
    }

    private void handleClick(Point p) {
        int idx = indexAt(p);
        if (idx < 0) return;
        int id = cardIds.get(idx);
        selectedCardId = (selectedCardId == id) ? -1 : id;
        repaint();
        if (listener != null) listener.onCardSelected(selectedCardId);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        Renderer.hints(g);
        cardBounds.clear();
        cardIds.clear();

        GameStateView state = controller.state();
        PlayerView me = state.currentPlayer();
        List<CardView> hand = (me != null) ? me.hand() : null;

        // Header strip.
        g.setColor(UiColors.PANEL_BG_2);
        g.fillRect(0, 0, getWidth(), 22);
        g.setColor(UiColors.TEXT_MUTED);
        g.setFont(getFont().deriveFont(Font.BOLD, 12f));
        String who = (me != null) ? me.name() + " — Hand (" + (hand == null ? 0 : hand.size()) + ")"
                                  : "Hand";
        g.drawString(who, 10, 16);

        if (hand == null || hand.isEmpty()) {
            g.setColor(UiColors.TEXT_MUTED);
            g.setFont(getFont().deriveFont(Font.ITALIC, 14f));
            g.drawString("No cards in hand.", 16, getHeight() / 2);
            return;
        }

        int top = 30;
        int cardH = getHeight() - top - 14;
        cardH = Math.max(90, Math.min(cardH, 150));
        int cardW = (int) (cardH * 0.68);
        int n = hand.size();
        int gap = 12;
        int totalW = n * cardW + (n - 1) * gap;
        int startX = Math.max(12, (getWidth() - totalW) / 2);
        // If overflowing, overlap the cards fan-style.
        int step = cardW + gap;
        if (totalW > getWidth() - 24 && n > 1) {
            step = (getWidth() - 24 - cardW) / (n - 1);
            startX = 12;
        }

        for (int i = 0; i < n; i++) {
            CardView card = hand.get(i);
            int x = startX + i * step;
            boolean selected = card.id() == selectedCardId;
            boolean hover = i == hoverIndex;
            int y = top + (selected ? -12 : (hover ? -5 : 0));
            Rectangle r = new Rectangle(x, top, cardW, cardH);
            cardBounds.add(r);
            cardIds.add(card.id());
            drawCard(g, card, x, y, cardW, cardH, selected, hover);
        }
    }

    private void drawCard(Graphics2D g, CardView card, int x, int y, int w, int h,
                          boolean selected, boolean hover) {
        Color accent = UiColors.card(card.type());
        // Drop shadow.
        g.setColor(UiColors.withAlpha(Color.BLACK, selected ? 120 : 70));
        g.fillRoundRect(x + 3, y + 5, w, h, 14, 14);

        // Card body.
        GradientPaint gp = new GradientPaint(x, y, new Color(0xF2ECDD),
                x, y + h, new Color(0xD9CFBA));
        g.setPaint(gp);
        g.fillRoundRect(x, y, w, h, 14, 14);

        // Accent header band.
        g.setColor(accent);
        g.fillRoundRect(x, y, w, 26, 14, 14);
        g.fillRect(x, y + 13, w, 13);

        // Border (glow if selected).
        if (selected) {
            g.setStroke(new BasicStroke(3f));
            g.setColor(UiColors.SELECT_GLOW);
        } else {
            g.setStroke(new BasicStroke(1.5f));
            g.setColor(UiColors.mix(accent, Color.BLACK, 0.4));
        }
        g.drawRoundRect(x, y, w, h, 14, 14);

        // Cost coin (top-left).
        if (card.cost() > 0) {
            g.setColor(UiColors.GOLD);
            g.fillOval(x + 4, y + 4, 18, 18);
            g.setColor(UiColors.TEXT_DARK);
            g.setFont(getFont().deriveFont(Font.BOLD, 11f));
            String cs = Integer.toString(card.cost());
            FontMetrics fm = g.getFontMetrics();
            g.drawString(cs, x + 4 + 9 - fm.stringWidth(cs) / 2, y + 4 + 13);
        }

        // Type label in header.
        g.setColor(UiColors.TEXT_DARK);
        g.setFont(getFont().deriveFont(Font.BOLD, 9f));
        String typeLabel = card.type().name();
        FontMetrics fm = g.getFontMetrics();
        g.drawString(typeLabel, x + w - fm.stringWidth(typeLabel) - 6, y + 17);

        // Central art zone — draw the icon relevant to this card type.
        int artCy = y + h / 2 - 4;
        double artR = Math.min(w, h) * 0.20;
        drawCardArt(g, card, x + w / 2.0, artCy, artR);

        // Name.
        g.setColor(UiColors.TEXT_DARK);
        g.setFont(getFont().deriveFont(Font.BOLD, 11f));
        drawClipped(g, card.name(), x + 6, y + h - 26, w - 12);

        // Stat line.
        g.setFont(getFont().deriveFont(Font.PLAIN, 10f));
        g.setColor(UiColors.mix(UiColors.TEXT_DARK, Color.DARK_GRAY, 0.3));
        String stat = statLine(card);
        if (stat != null) drawClipped(g, stat, x + 6, y + h - 10, w - 12);
    }

    private void drawCardArt(Graphics2D g, CardView card, double cx, double cy, double r) {
        CardType t = card.type();
        if (t == CardType.BUILDING && card.buildingType() != null) {
            Renderer.building(g, card.buildingType(), cx, cy, r);
        } else if (t == CardType.ATTACHMENT && card.attachmentType() != null) {
            Renderer.attachment(g, card.attachmentType(), cx, cy, r * 0.9);
        } else if (t == CardType.MILITARY) {
            Renderer.raider(g, cx, cy, r * 0.7, 0); // crossed swords motif
        } else if (t == CardType.ECONOMY) {
            g.setColor(UiColors.GOLD);
            g.fillOval((int) (cx - r), (int) (cy - r), (int) (r * 2), (int) (r * 2));
            g.setColor(UiColors.mix(UiColors.GOLD, Color.BLACK, 0.4));
            g.setStroke(new BasicStroke(2f));
            g.drawOval((int) (cx - r), (int) (cy - r), (int) (r * 2), (int) (r * 2));
            g.setFont(getFont().deriveFont(Font.BOLD, (float) r));
            g.drawString("$", (int) (cx - r * 0.3), (int) (cy + r * 0.4));
        } else if (t == CardType.EXPLORE) {
            g.setColor(UiColors.card(CardType.EXPLORE));
            g.setStroke(new BasicStroke(2.5f));
            g.drawOval((int) (cx - r), (int) (cy - r), (int) (r * 2), (int) (r * 2));
            g.drawLine((int) (cx + r * 0.7), (int) (cy + r * 0.7),
                    (int) (cx + r * 1.5), (int) (cy + r * 1.5));
        } else if (t == CardType.SIN) {
            g.setColor(UiColors.card(CardType.SIN));
            java.awt.Polygon sk = Renderer.star(cx, cy, r, r * 0.4, 6, 0);
            g.fill(sk);
            g.setColor(Color.BLACK);
            g.drawString("✖", (int) (cx - r * 0.4), (int) (cy + r * 0.4));
        }
    }

    private String statLine(CardView card) {
        CardType t = card.type();
        if (t == CardType.MILITARY) return "Attack " + card.attack();
        if (t == CardType.BUILDING && card.buildingType() != null)
            return "Build " + card.buildingType().display();
        if (t == CardType.ATTACHMENT && card.attachmentType() != null)
            return "Attach " + card.attachmentType().display();
        if (t == CardType.SIN) return "Unplayable — discard";
        String d = card.description();
        return d != null && d.length() > 0 ? d : t.name();
    }

    private void drawClipped(Graphics2D g, String text, int x, int y, int maxW) {
        if (text == null) return;
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(text) <= maxW) {
            g.drawString(text, x, y);
            return;
        }
        String ell = "…";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (fm.stringWidth(sb.toString() + text.charAt(i) + ell) > maxW) break;
            sb.append(text.charAt(i));
        }
        g.drawString(sb.toString() + ell, x, y);
    }
}
