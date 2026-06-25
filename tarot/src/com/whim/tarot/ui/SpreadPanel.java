package com.whim.tarot.ui;

import com.whim.tarot.domain.SpreadType;
import com.whim.tarot.engine.PositionedCard;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 * Canvas that arranges the cards of a {@link com.whim.tarot.engine.Reading} to
 * match the physical shape of its spread, using absolute (null-layout)
 * positioning. The Celtic Cross overlaps card 0 and the 90-degree crossing
 * card 1 at the centre, surrounds them with cards 2-5, and stacks cards 6-9 as
 * a vertical staff column on the far right (6 bottom -> 9 top).
 */
final class SpreadPanel extends JPanel {

    private static final Color FELT = new Color(0x1B, 0x2A, 0x22);

    /** Listener notified when a card is clicked. */
    interface CardSelectionListener {
        void onCardSelected(CardView view);
    }

    private final CardSelectionListener listener;
    private final List<CardView> views = new ArrayList<CardView>();
    private CardView selected;

    SpreadPanel(CardSelectionListener listener) {
        this.listener = listener;
        setLayout(null);
        setBackground(FELT);
        setPreferredSize(new Dimension(820, 620));
    }

    /** Replaces all displayed cards with the given reading's layout. */
    void showReading(SpreadType type, List<PositionedCard> cards) {
        removeAll();
        views.clear();
        selected = null;

        for (int i = 0; i < cards.size(); i++) {
            boolean crossing = (type == SpreadType.CELTIC_CROSS && i == 1);
            final CardView view = new CardView(cards.get(i), crossing);
            view.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    select(view);
                }
            });
            views.add(view);
            add(view);
        }

        Dimension needed;
        switch (type) {
            case SINGLE:
                needed = layoutSingle();
                break;
            case THREE_CARD:
                needed = layoutThreeCard();
                break;
            case CELTIC_CROSS:
            default:
                needed = layoutCelticCross();
                break;
        }
        setPreferredSize(needed);
        revalidate();
        repaint();

        if (!views.isEmpty()) {
            select(views.get(0));
        }
    }

    /** The card views currently displayed, in deal order. */
    List<CardView> getViews() {
        return views;
    }

    private void select(CardView view) {
        if (selected != null) {
            selected.setSelected(false);
        }
        selected = view;
        view.setSelected(true);
        // Crossing card paints over its neighbour; keep it on top after selection.
        if (listener != null) {
            listener.onCardSelected(view);
        }
    }

    // --- layout helpers -------------------------------------------------

    /** Places a card's centre at (cx, cy). Crossing cards keep their landscape box. */
    private void placeCentered(int idx, int cx, int cy) {
        CardView v = views.get(idx);
        Dimension d = v.getPreferredSize();
        v.setBounds(cx - d.width / 2, cy - d.height / 2, d.width, d.height);
    }

    private Dimension layoutSingle() {
        int cx = 410;
        int cy = 300;
        placeCentered(0, cx, cy);
        return new Dimension(820, 600);
    }

    private Dimension layoutThreeCard() {
        int cy = 300;
        int gap = 60;
        int step = CardView.CARD_W + gap;
        int cx0 = 410 - step;
        for (int i = 0; i < views.size(); i++) {
            placeCentered(i, cx0 + i * step, cy);
        }
        return new Dimension(820, 600);
    }

    private Dimension layoutCelticCross() {
        int cx = 280;        // centre of the cross
        int cy = 310;
        int hArm = 165;      // centre->left/right neighbour
        int vArm = 205;      // centre->above/below neighbour

        // Cross: 0 centre, 1 crossing (rotated 90), 2 below, 3 left, 4 above, 5 right.
        placeCentered(0, cx, cy);
        if (views.size() > 1) placeCentered(1, cx, cy);
        if (views.size() > 2) placeCentered(2, cx, cy + vArm);
        if (views.size() > 3) placeCentered(3, cx - hArm, cy);
        if (views.size() > 4) placeCentered(4, cx, cy - vArm);
        if (views.size() > 5) placeCentered(5, cx + hArm, cy);

        // Staff column on the far right: 6 bottom -> 9 top.
        int staffX = cx + hArm + CardView.CARD_W / 2 + 130;
        int rowH = CardView.CARD_H + 18;
        int staffTopCy = cy - (int) (1.5 * rowH);
        for (int i = 6; i < views.size(); i++) {
            int rowFromTop = 9 - i; // i=9 -> top row 0, i=6 -> bottom row 3
            placeCentered(i, staffX, staffTopCy + rowFromTop * rowH);
        }

        // Ensure the crossing card paints above card 0 (added later == lower z by
        // default in null layout; move it to front explicitly).
        if (views.size() > 1) {
            setComponentZOrder(views.get(1), 0);
        }

        int width = staffX + CardView.CARD_W / 2 + 30;
        int height = cy + (int) (1.5 * rowH) + CardView.CARD_H / 2 + 30;
        return new Dimension(Math.max(width, 820), Math.max(height, 620));
    }
}
