package com.whim.firetop.ui;

import com.whim.firetop.model.Character;
import com.whim.firetop.model.GameState;
import com.whim.firetop.model.Item;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

/**
 * Right-hand panel showing every adventurer's SKILL/STAMINA/LUCK meters, gold,
 * provisions and inventory. The active adventurer's card is highlighted.
 */
public final class StatusPanel extends JPanel {

    private GameState state;

    public StatusPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Theme.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    public void setState(GameState state) {
        this.state = state;
        rebuild();
    }

    public void refresh() { rebuild(); }

    private void rebuild() {
        removeAll();
        if (state != null) {
            for (int i = 0; i < state.getPlayers().size(); i++) {
                add(buildCard(state.getPlayers().get(i), i, i == state.getCurrentPlayerIndex()));
            }
        }
        revalidate();
        repaint();
    }

    private JPanel buildCard(Character c, int idx, boolean active) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout(4, 4));
        card.setBackground(active ? new Color(52, 44, 60) : Theme.BG_PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(active ? Theme.GOLD : Theme.STONE, active ? 2 : 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        card.setMaximumSize(new Dimension(280, 240));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel dot = new JLabel("⬤");
        dot.setForeground(c.isAlive() ? Theme.PLAYER_COLORS[idx % 4] : Color.DARK_GRAY);
        header.add(dot, BorderLayout.WEST);
        JLabel title = new JLabel(" " + (idx + 1) + ". " + c.getName()
                + (c.isAlive() ? (active ? "  (active)" : "") : "  (fallen)"));
        title.setForeground(Theme.PARCHMENT);
        title.setFont(Theme.HEADING);
        header.add(title, BorderLayout.CENTER);
        card.add(header, BorderLayout.NORTH);

        JPanel meters = new JPanel(new GridLayout(3, 1, 0, 4));
        meters.setOpaque(false);
        meters.add(new Meter("SKILL", c.getSkillCurrent(), c.getSkillInitial(), Theme.ROYAL));
        meters.add(new Meter("STAMINA", c.getStaminaCurrent(), c.getStaminaInitial(), Theme.BLOOD));
        meters.add(new Meter("LUCK", c.getLuckCurrent(), c.getLuckInitial(), Theme.GOLD));
        card.add(meters, BorderLayout.CENTER);

        StringBuilder inv = new StringBuilder("<html>");
        inv.append("Gold: ").append(c.getGold())
                .append("&nbsp;&nbsp;Provisions: ").append(c.getProvisions()).append("<br>");
        inv.append("Pack: ");
        if (c.getInventory().isEmpty()) {
            inv.append("<i>empty</i>");
        } else {
            for (int i = 0; i < c.getInventory().size(); i++) {
                Item it = c.getInventory().get(i);
                if (i > 0) {
                    inv.append(", ");
                }
                inv.append(it.getName());
            }
        }
        inv.append("</html>");
        JLabel invLabel = new JLabel(inv.toString());
        invLabel.setForeground(Theme.STONE_LIGHT);
        invLabel.setFont(Theme.BODY);
        card.add(invLabel, BorderLayout.SOUTH);
        return card;
    }

    /** A labelled value bar. */
    private static final class Meter extends JPanel {
        private final String label;
        private final int value;
        private final int max;
        private final Color color;

        Meter(String label, int value, int max, Color color) {
            this.label = label;
            this.value = value;
            this.max = Math.max(1, max);
            this.color = color;
            setOpaque(false);
            setPreferredSize(new Dimension(240, 20));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int barX = 74;
            int barW = w - barX - 44;
            g2.setColor(Theme.PARCHMENT);
            g2.setFont(Theme.BODY_BOLD);
            g2.drawString(label, 0, h - 5);
            g2.setColor(Theme.STONE.darker());
            g2.fillRoundRect(barX, 3, barW, h - 8, 8, 8);
            int filled = (int) Math.round(barW * Math.min(1.0, value / (double) max));
            g2.setColor(color);
            g2.fillRoundRect(barX, 3, filled, h - 8, 8, 8);
            g2.setColor(Theme.STONE_LIGHT);
            g2.drawRoundRect(barX, 3, barW, h - 8, 8, 8);
            g2.setColor(Theme.PARCHMENT);
            g2.drawString(value + "/" + max, barX + barW + 6, h - 5);
        }
    }
}
