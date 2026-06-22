package com.whim.babylon5.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.whim.babylon5.domain.Card;
import com.whim.babylon5.domain.PlayerState;
import com.whim.babylon5.domain.ZoneType;

/**
 * One player's region of the tabletop: a header with name/faction/influence/power,
 * the Inner Circle row (Ambassador + key characters) and the Supporting row.
 * Cards in the human player's hand are rendered separately by {@link MainWindow}.
 */
final class PlayerPanel extends JPanel {

    private final PlayerState player;
    private final boolean active;
    private final CardView.SelectionListener selectionListener;
    private final JLabel header = new JLabel();
    private final JPanel inner = row();
    private final JPanel supporting = row();

    PlayerPanel(PlayerState player, boolean active, int power,
                CardView.SelectionListener selectionListener) {
        this.player = player;
        this.active = active;
        this.selectionListener = selectionListener;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        header.setForeground(UiTheme.INK);
        header.setFont(UiTheme.H2);
        header.setText(buildHeader(power));
        add(header);
        add(label("Inner Circle"));
        add(inner);
        add(label("Supporting"));
        add(supporting);

        fill(inner, player.zone(ZoneType.INNER_CIRCLE));
        fill(supporting, player.zone(ZoneType.SUPPORTING));
    }

    private String buildHeader(int power) {
        return "<html><span style='color:#" + hex(UiTheme.factionColor(player.getFaction())) + "'>"
                + (active ? "▶ " : "") + esc(player.getName()) + "</span> &nbsp; "
                + player.getFaction() + " &nbsp; | &nbsp; Influence "
                + player.getInfluencePool() + "/" + player.getInfluenceRating()
                + " &nbsp; | &nbsp; <b>Power " + power + "</b>/20</html>";
    }

    private void fill(JPanel rowPanel, com.whim.babylon5.domain.Zone zone) {
        rowPanel.removeAll();
        if (zone == null || zone.isEmpty()) {
            JLabel empty = new JLabel("—");
            empty.setForeground(UiTheme.INK_DIM);
            rowPanel.add(empty);
            return;
        }
        for (Card c : zone.getCards()) {
            CardView v = new CardView(c);
            v.setSelectionListener(selectionListener);
            rowPanel.add(v);
        }
    }

    private static JPanel row() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        p.setOpaque(false);
        return p;
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(UiTheme.INK_DIM);
        l.setFont(UiTheme.BODY);
        return l;
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(active ? UiTheme.PANEL_HI : UiTheme.PANEL);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        if (active) {
            g2.setColor(UiTheme.ACCENT);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        }
        g2.dispose();
        super.paintComponent(g);
    }

    @Override public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    private static String hex(Color c) {
        return String.format("%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("<", "&lt;").replace(">", "&gt;");
    }

    static Box.Filler glue() {
        return (Box.Filler) Box.createVerticalGlue();
    }
}
