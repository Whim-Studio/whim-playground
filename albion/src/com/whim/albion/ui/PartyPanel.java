package com.whim.albion.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JPanel;

import com.whim.albion.api.GameController;
import com.whim.albion.api.Views.CharacterView;
import com.whim.albion.api.Views.PartyView;
import com.whim.albion.api.Views.WorldView;
import com.whim.albion.api.Enums.Direction;

/**
 * EAST chrome: party portrait strip with LP/SP bars plus a minimap and compass. Clicking a
 * portrait selects the active member; double-click opens their character sheet.
 */
final class PartyPanel extends JPanel {

    private final GameController controller;
    private int rowH = 64;

    PartyPanel(GameController controller) {
        this.controller = controller;
        setBackground(UiUtil.PANEL_BG);
        setPreferredSize(new Dimension(220, 600));
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                int idx = e.getY() / rowH;
                PartyView pv = controller.state().party();
                if (pv != null && idx >= 0 && idx < pv.members().size()) {
                    controller.setActiveMember(idx);
                    if (e.getClickCount() >= 2) {
                        controller.openState(com.whim.albion.api.Enums.GameStateType.CHARACTER_SHEET);
                    }
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        PartyView pv = controller.state().party();
        if (pv == null) return;
        List<CharacterView> members = pv.members();
        int active = pv.activeIndex();

        for (int i = 0; i < members.size(); i++) {
            CharacterView c = members.get(i);
            int y = i * rowH;
            if (i == active) {
                g.setColor(new Color(70, 64, 46));
                g.fillRect(0, y, getWidth(), rowH);
            }
            // portrait
            SpriteFactory.drawPortrait(g, c.portraitKey(), 4, y + 4, 52, rowH - 10);
            // name / profession
            g.setColor(c.alive() ? UiUtil.INK : new Color(150, 90, 90));
            g.setFont(UiUtil.UI_BOLD);
            g.drawString(c.name() + "  (Lv" + c.level() + ")", 62, y + 16);
            g.setColor(new Color(170, 160, 140));
            g.setFont(UiUtil.UI_FONT);
            g.drawString(c.profession(), 62, y + 30);
            // bars
            UiUtil.bar(g, 62, y + 36, getWidth() - 72, 10, c.lp(), c.maxLp(), UiUtil.LP_COLOR, "LP");
            UiUtil.bar(g, 62, y + 48, getWidth() - 72, 10, c.sp(), c.maxSp(), UiUtil.SP_COLOR, "SP");
            g.setColor(new Color(0, 0, 0, 70));
            g.drawLine(0, y + rowH - 1, getWidth(), y + rowH - 1);
        }

        int mapTop = members.size() * rowH + 6;
        drawMinimap(g, mapTop);
        drawCompass(g, getWidth() - 44, getHeight() - 48);
    }

    private void drawMinimap(Graphics2D g, int top) {
        WorldView w = controller.state().world();
        int size = getWidth() - 12;
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(6, top, size, size);
        g.setColor(UiUtil.PANEL_EDGE);
        g.drawRect(6, top, size, size);
        if (w == null) {
            g.setColor(new Color(120, 110, 100));
            g.drawString("(no map)", 14, top + 20);
            return;
        }
        int cell = Math.max(2, size / Math.max(w.width(), w.height()));
        for (int y = 0; y < w.height(); y++) {
            for (int x = 0; x < w.width(); x++) {
                g.setColor(w.tileAt(x, y).walkable() ? new Color(90, 110, 90) : new Color(50, 46, 44));
                g.fillRect(6 + x * cell, top + y * cell, cell, cell);
            }
        }
        g.setColor(new Color(240, 220, 120));
        g.fillOval(6 + w.player().x() * cell, top + w.player().y() * cell, cell, cell);
    }

    private void drawCompass(Graphics2D g, int cx, int cy) {
        WorldView w = controller.state().world();
        Direction f = (w == null) ? Direction.NORTH : w.player().facing();
        g.setColor(new Color(0, 0, 0, 150));
        g.fillOval(cx - 18, cy - 18, 36, 36);
        g.setColor(UiUtil.PANEL_EDGE);
        g.drawOval(cx - 18, cy - 18, 36, 36);
        g.setColor(new Color(220, 80, 80));
        g.drawLine(cx, cy, cx + f.dx() * 14, cy + f.dy() * 14);
        g.setColor(UiUtil.INK);
        g.drawString("N", cx - 4, cy - 20);
    }
}
