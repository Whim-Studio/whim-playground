package com.whim.cardwoven.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import javax.swing.JPanel;

import com.whim.cardwoven.api.Enums.ResourceType;
import com.whim.cardwoven.api.Enums.VictoryType;
import com.whim.cardwoven.api.GameController;
import com.whim.cardwoven.api.Views.GameStateView;
import com.whim.cardwoven.api.Views.PlayerView;

/**
 * Left-hand readout: faction/turn/phase header, Gold & Command Points, Deck &
 * Discard counts, and a victory-progress bar per pursuable victory type. Drawn
 * with Graphics2D; re-reads state on every paint so it stays in sync.
 */
public class SidePanel extends JPanel {

    private final GameController controller;

    public SidePanel(GameController controller) {
        this.controller = controller;
        setBackground(UiColors.PANEL_BG);
        setPreferredSize(new Dimension(230, 460));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        Renderer.hints(g);

        GameStateView state = controller.state();
        PlayerView me = state.currentPlayer();
        int w = getWidth();
        int y = 16;

        // ---- Header ---------------------------------------------------
        g.setColor(UiColors.TEXT);
        g.setFont(getFont().deriveFont(Font.BOLD, 16f));
        String faction = me != null ? me.faction().display() : "—";
        g.drawString(faction, 14, y + 4);
        y += 22;
        g.setColor(UiColors.TEXT_MUTED);
        g.setFont(getFont().deriveFont(Font.PLAIN, 12f));
        g.drawString("Turn " + state.turnNumber() + "  ·  " + state.phase().name(), 14, y);
        y += 22;

        if (state.isGameOver()) {
            g.setColor(UiColors.SELECT_GLOW);
            g.setFont(getFont().deriveFont(Font.BOLD, 13f));
            String vt = state.winningVictory() != null ? state.winningVictory().display() : "";
            g.drawString("VICTORY: " + vt, 14, y);
            y += 20;
        }

        y = section(g, "Resources", y, w);
        if (me != null) {
            y = resourceRow(g, "Gold", me.resource(ResourceType.GOLD), UiColors.GOLD, y, w);
            y = resourceRow(g, "Command", me.resource(ResourceType.COMMAND_POINTS),
                    UiColors.COMMAND, y, w);
        }
        y += 6;

        y = section(g, "Cards", y, w);
        if (me != null) {
            y = resourceRow(g, "Deck", me.deckCount(), UiColors.DECK, y, w);
            y = resourceRow(g, "Discard", me.discardCount(), UiColors.DISCARD, y, w);
            y = resourceRow(g, "Hand", me.handSize(), UiColors.TEXT_MUTED, y, w);
        }
        y += 6;

        y = section(g, "Victory Progress", y, w);
        if (me != null) {
            List<VictoryType> paths = me.pursuableVictories();
            if (paths == null || paths.isEmpty()) {
                g.setColor(UiColors.TEXT_MUTED);
                g.setFont(getFont().deriveFont(Font.ITALIC, 11f));
                g.drawString("No active paths", 16, y + 4);
                y += 18;
            } else {
                for (int i = 0; i < paths.size(); i++) {
                    VictoryType vt = paths.get(i);
                    double p = me.victoryProgress(vt);
                    g.setColor(UiColors.TEXT);
                    g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
                    g.drawString(vt.display(), 16, y);
                    g.setColor(UiColors.TEXT_MUTED);
                    String pct = Math.round(p * 100) + "%";
                    g.drawString(pct, w - 14 - g.getFontMetrics().stringWidth(pct), y);
                    y += 5;
                    Renderer.progressBar(g, 16, y, w - 32, 12, p,
                            victoryColor(vt), null, UiColors.TEXT_DARK);
                    y += 22;
                }
            }
        }
    }

    private int section(Graphics2D g, String title, int y, int w) {
        y += 10;
        g.setColor(UiColors.TEXT_MUTED);
        g.setFont(getFont().deriveFont(Font.BOLD, 11f));
        g.drawString(title.toUpperCase(), 14, y);
        y += 4;
        g.setColor(UiColors.GRID_LINE);
        g.drawLine(14, y, w - 14, y);
        return y + 14;
    }

    private int resourceRow(Graphics2D g, String label, int value, Color color, int y, int w) {
        g.setColor(color);
        g.fillRoundRect(14, y - 10, 12, 12, 4, 4);
        g.setColor(UiColors.TEXT);
        g.setFont(getFont().deriveFont(Font.PLAIN, 13f));
        g.drawString(label, 34, y);
        g.setFont(getFont().deriveFont(Font.BOLD, 14f));
        String v = Integer.toString(value);
        g.drawString(v, w - 14 - g.getFontMetrics().stringWidth(v), y);
        return y + 22;
    }

    private Color victoryColor(VictoryType vt) {
        switch (vt) {
            case ECONOMIC:  return UiColors.GOLD;
            case MILITARY:  return UiColors.RAIDER;
            case EXPANSION: return UiColors.DECK;
            case FAITH:     return new Color(0xC9A2E8);
            case DOMINANCE: return UiColors.COMMAND;
            default:        return UiColors.TEXT_MUTED;
        }
    }
}
