package com.whim.cardwoven.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.whim.cardwoven.api.GameController;
import com.whim.cardwoven.api.Views.GameStateView;

/**
 * Recent-event feed drawn along the right side. Reads {@code state().recentLog()}
 * and shows the newest entries at the top, wrapping long lines. Optionally holds
 * a transient status message set by the frame after an action.
 */
public class LogPanel extends JPanel {

    private final GameController controller;
    private String status;
    private boolean statusOk = true;

    public LogPanel(GameController controller) {
        this.controller = controller;
        setBackground(UiColors.PANEL_BG);
        setPreferredSize(new Dimension(230, 200));
    }

    public void setStatus(String msg, boolean ok) {
        this.status = msg;
        this.statusOk = ok;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        Renderer.hints(g);

        int w = getWidth();
        int y = 18;
        g.setColor(UiColors.TEXT_MUTED);
        g.setFont(getFont().deriveFont(Font.BOLD, 11f));
        g.drawString("EVENT LOG", 14, y);
        y += 4;
        g.setColor(UiColors.GRID_LINE);
        g.drawLine(14, y, w - 14, y);
        y += 16;

        if (status != null) {
            g.setColor(statusOk ? UiColors.DECK : UiColors.RAIDER);
            g.setFont(getFont().deriveFont(Font.BOLD, 11f));
            y = drawWrapped(g, "» " + status, 14, y, w - 24) + 6;
        }

        GameStateView state = controller.state();
        List<String> log = state.recentLog();
        g.setFont(getFont().deriveFont(Font.PLAIN, 11f));
        if (log == null || log.isEmpty()) {
            g.setColor(UiColors.TEXT_MUTED);
            g.drawString("No events yet.", 16, y);
            return;
        }
        // Newest first.
        List<String> lines = new ArrayList<String>(log);
        int max = getHeight();
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (y > max - 4) break;
            g.setColor(i == lines.size() - 1 ? UiColors.TEXT : UiColors.TEXT_MUTED);
            y = drawWrapped(g, "· " + lines.get(i), 14, y, w - 24) + 3;
        }
    }

    private int drawWrapped(Graphics2D g, String text, int x, int y, int maxW) {
        FontMetrics fm = g.getFontMetrics();
        int lineH = fm.getHeight();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String test = line.length() == 0 ? words[i] : line + " " + words[i];
            if (fm.stringWidth(test) > maxW && line.length() > 0) {
                g.drawString(line.toString(), x, y);
                y += lineH;
                line = new StringBuilder(words[i]);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) {
            g.drawString(line.toString(), x, y);
            y += lineH;
        }
        return y;
    }
}
