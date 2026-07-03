package com.whim.ttr.ui;

import com.whim.ttr.api.CardColor;
import com.whim.ttr.domain.DestinationTicket;
import com.whim.ttr.domain.GameState;
import com.whim.ttr.domain.Player;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The bottom dashboard: the active player's hand (grouped by color with counts),
 * their destination tickets, the five face-up market cards plus the draw deck
 * (all click-to-draw), trains/stations remaining, score, the latest status
 * message, and turn-control buttons.
 */
public class DashboardPanel extends JPanel {

    /** Player-initiated actions routed to the {@link GameFrame} worker. */
    public interface Listener {
        void onDrawFaceUp(int index);
        void onDrawBlind();
        void onDrawTickets();
        void onEndTurn();
    }

    private final GameState state;
    private Listener listener;

    private final JLabel headerLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final JPanel handPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
    private final JPanel ticketPanel = new JPanel();
    private final JPanel marketPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));

    public DashboardPanel(GameState state) {
        this.state = state;
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8));
        setBackground(new Color(236, 232, 222));
        setPreferredSize(new Dimension(1000, 210));
        build();
    }

    public void setListener(Listener l) {
        this.listener = l;
    }

    private void build() {
        // --- top strip: header + status ---
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 15f));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC, 13f));
        statusLabel.setForeground(new Color(70, 40, 20));
        top.add(headerLabel, BorderLayout.WEST);
        top.add(statusLabel, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // --- center: hand + tickets ---
        JPanel center = new JPanel(new BorderLayout(8, 4));
        center.setOpaque(false);

        handPanel.setOpaque(false);
        handPanel.setBorder(BorderFactory.createTitledBorder("Your train cards"));

        ticketPanel.setLayout(new BoxLayout(ticketPanel, BoxLayout.Y_AXIS));
        ticketPanel.setOpaque(false);
        ticketPanel.setBorder(BorderFactory.createTitledBorder("Your tickets"));
        ticketPanel.setPreferredSize(new Dimension(300, 120));

        center.add(handPanel, BorderLayout.CENTER);
        center.add(ticketPanel, BorderLayout.EAST);
        add(center, BorderLayout.CENTER);

        // --- bottom: market + controls ---
        JPanel bottom = new JPanel(new BorderLayout(8, 4));
        bottom.setOpaque(false);
        marketPanel.setOpaque(false);
        marketPanel.setBorder(BorderFactory.createTitledBorder("Card market — click to draw"));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        controls.setOpaque(false);
        JButton drawTickets = new JButton("Draw Tickets");
        JButton endTurn = new JButton("End Turn");
        drawTickets.addActionListener(e -> { if (listener != null) listener.onDrawTickets(); });
        endTurn.addActionListener(e -> { if (listener != null) listener.onEndTurn(); });
        controls.add(drawTickets);
        controls.add(endTurn);

        bottom.add(marketPanel, BorderLayout.CENTER);
        bottom.add(controls, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        refresh();
    }

    /** Rebuild all widgets from the current game state. Call on the EDT. */
    public void refresh() {
        Player p = state.player(state.currentPlayerId());
        if (p != null) {
            headerLabel.setText("Turn: " + p.name()
                    + "   —   trains " + p.trainsLeft()
                    + " | stations " + p.stationsLeft()
                    + " | score " + p.score());
            headerLabel.setForeground(darken(p.token()));
        }
        String msg = state.lastMessage();
        statusLabel.setText(msg == null ? "" : msg);

        rebuildHand(p);
        rebuildTickets(p);
        rebuildMarket();

        revalidate();
        repaint();
    }

    private void rebuildHand(Player p) {
        handPanel.removeAll();
        if (p == null) {
            return;
        }
        Map<CardColor, Integer> counts = new EnumMap<CardColor, Integer>(CardColor.class);
        for (CardColor c : p.hand()) {
            Integer cur = counts.get(c);
            counts.put(c, (cur == null ? 0 : cur) + 1);
        }
        boolean any = false;
        for (CardColor c : orderedColors()) {
            Integer n = counts.get(c);
            if (n == null || n == 0) {
                continue;
            }
            any = true;
            handPanel.add(chip(UiColors.label(c) + "  ×" + n, UiColors.of(c), UiColors.textOn(c), 96));
        }
        if (!any) {
            handPanel.add(new JLabel("(no cards)"));
        }
    }

    private void rebuildTickets(Player p) {
        ticketPanel.removeAll();
        if (p == null) {
            return;
        }
        List<DestinationTicket> tickets = p.tickets();
        if (tickets == null || tickets.isEmpty()) {
            ticketPanel.add(new JLabel("(no tickets)"));
            return;
        }
        for (DestinationTicket t : tickets) {
            JLabel l = new JLabel(t.from() + " → " + t.to() + "  (" + t.points() + ")");
            l.setFont(l.getFont().deriveFont(Font.PLAIN, 12f));
            ticketPanel.add(l);
        }
    }

    private void rebuildMarket() {
        marketPanel.removeAll();
        List<CardColor> faceUp = state.deck() == null ? null : state.deck().faceUp();
        if (faceUp != null) {
            for (int i = 0; i < faceUp.size(); i++) {
                final int idx = i;
                CardColor c = faceUp.get(i);
                JComponent chip = chip(UiColors.label(c), UiColors.of(c), UiColors.textOn(c), 78);
                chip.setToolTipText("Draw this face-up card");
                chip.addMouseListener(new MouseAdapter() {
                    @Override public void mousePressed(MouseEvent e) {
                        if (listener != null) listener.onDrawFaceUp(idx);
                    }
                });
                marketPanel.add(chip);
            }
        }
        JComponent deck = chip("DECK", new Color(35, 60, 90), Color.WHITE, 78);
        deck.setToolTipText("Draw a blind card from the deck");
        deck.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (listener != null) listener.onDrawBlind();
            }
        });
        marketPanel.add(deck);
    }

    // ---- helpers ------------------------------------------------------------

    private static JLabel chip(String text, Color bg, Color fg, int width) {
        JLabel l = new JLabel(text, JLabel.CENTER);
        l.setOpaque(true);
        l.setBackground(bg);
        l.setForeground(fg);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
        l.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60), 1, true));
        l.setPreferredSize(new Dimension(width, 40));
        return l;
    }

    private static CardColor[] orderedColors() {
        CardColor[] train = CardColor.trainColors();
        CardColor[] all = new CardColor[train.length + 1];
        System.arraycopy(train, 0, all, 0, train.length);
        all[train.length] = CardColor.LOCOMOTIVE;
        return all;
    }

    private static Color darken(Color c) {
        if (c == null) {
            return Color.DARK_GRAY;
        }
        return c.darker().darker();
    }
}
