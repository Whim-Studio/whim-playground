package com.whim.albion.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JPanel;

import com.whim.albion.api.GameController;

/** TITLE / GAME_OVER menu screen driven by {@link com.whim.albion.api.Views.GameStateView#menuOptions()}. */
final class TitleScreen extends JPanel {

    private final GameController controller;
    private int hover = -1;

    TitleScreen(GameController controller) {
        this.controller = controller;
        setBackground(new Color(10, 12, 20));
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                int idx = optionAt(e.getY());
                if (idx >= 0) controller.selectMenuOption(idx);
            }
            @Override public void mouseMoved(MouseEvent e) {
                int idx = optionAt(e.getY());
                if (idx != hover) { hover = idx; repaint(); }
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    private int optionsTop() { return getHeight() / 2; }

    private int optionAt(int y) {
        List<String> opts = controller.state().menuOptions();
        int top = optionsTop();
        for (int i = 0; i < opts.size(); i++) {
            int oy = top + i * 44;
            if (y >= oy - 22 && y < oy + 12) return i;
        }
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(14, 18, 34), 0, getHeight(), new Color(30, 20, 30)));
        g.fillRect(0, 0, getWidth(), getHeight());

        boolean gameOver = controller.state().current() == com.whim.albion.api.Enums.GameStateType.GAME_OVER;
        g.setFont(UiUtil.TITLE_FONT.deriveFont(56f));
        g.setColor(gameOver ? new Color(180, 70, 70) : new Color(230, 205, 130));
        String title = gameOver ? "Game Over" : "ALBION";
        int tw = g.getFontMetrics().stringWidth(title);
        g.drawString(title, getWidth() / 2 - tw / 2, getHeight() / 3);

        if (!gameOver) {
            g.setFont(UiUtil.UI_FONT.deriveFont(15f));
            g.setColor(new Color(170, 170, 180));
            String sub = "a clean-room Swing recreation — dev UI shell";
            int sw = g.getFontMetrics().stringWidth(sub);
            g.drawString(sub, getWidth() / 2 - sw / 2, getHeight() / 3 + 30);
        }

        List<String> opts = controller.state().menuOptions();
        int top = optionsTop();
        g.setFont(UiUtil.UI_BOLD.deriveFont(20f));
        for (int i = 0; i < opts.size(); i++) {
            int oy = top + i * 44;
            g.setColor(i == hover ? new Color(240, 220, 130) : new Color(200, 195, 180));
            String label = (i == hover ? "> " : "  ") + opts.get(i);
            int w = g.getFontMetrics().stringWidth(label);
            g.drawString(label, getWidth() / 2 - w / 2, oy);
        }
    }

    @Override public Dimension getPreferredSize() { return new Dimension(960, 640); }
}
