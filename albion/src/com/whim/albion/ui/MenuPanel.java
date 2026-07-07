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

/**
 * Save/Load menu overlay driven by {@link GameController#saveSlots()}. Left-click a slot to
 * load it; the buttons at the bottom save to the highlighted slot or close the menu.
 */
final class MenuPanel extends JPanel {

    private final GameController controller;
    private int hover = -1;

    MenuPanel(GameController controller) {
        this.controller = controller;
        setBackground(new Color(18, 18, 26));
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                int idx = slotAt(e.getY());
                if (idx >= 0) {
                    if (e.isShiftDown()) controller.saveGame(idx);
                    else controller.loadGame(idx);
                }
            }
            @Override public void mouseMoved(MouseEvent e) {
                int idx = slotAt(e.getY());
                if (idx != hover) { hover = idx; repaint(); }
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    private int slotAt(int y) {
        List<String> slots = controller.saveSlots();
        for (int i = 0; i < slots.size(); i++) {
            int oy = 90 + i * 50;
            if (y >= oy && y < oy + 44) return i;
        }
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(232, 220, 160));
        g.setFont(UiUtil.UI_BOLD.deriveFont(18f));
        g.drawString("Save / Load    (click = load · Shift+click = save · Esc to close)", 20, 40);

        List<String> slots = controller.saveSlots();
        for (int i = 0; i < slots.size(); i++) {
            int oy = 90 + i * 50;
            g.setColor(i == hover ? new Color(60, 56, 44) : new Color(34, 32, 40));
            g.fillRect(30, oy, getWidth() - 60, 44);
            g.setColor(UiUtil.PANEL_EDGE);
            g.drawRect(30, oy, getWidth() - 60, 44);
            g.setColor(UiUtil.INK);
            g.setFont(UiUtil.UI_FONT.deriveFont(14f));
            g.drawString(slots.get(i), 46, oy + 27);
        }
    }

    @Override public Dimension getPreferredSize() { return new Dimension(640, 560); }
}
