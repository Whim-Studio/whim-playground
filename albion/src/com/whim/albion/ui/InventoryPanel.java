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
import com.whim.albion.api.Views.ItemView;
import com.whim.albion.api.Views.PartyView;
import com.whim.albion.api.Enums.EquipSlot;

/**
 * Inventory overlay for the active party member: equipment slots on the left, the backpack
 * grid on the right. Click a backpack item to use/equip it; click an equipped slot to unequip.
 */
final class InventoryPanel extends JPanel {

    private final GameController controller;
    private static final int CELL = 56;
    private static final int COLS = 6;

    InventoryPanel(GameController controller) {
        this.controller = controller;
        setBackground(new Color(24, 22, 28));
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { onClick(e); }
        });
    }

    private CharacterView active() {
        PartyView pv = controller.state().party();
        if (pv == null || pv.members().isEmpty()) return null;
        int i = Math.max(0, Math.min(pv.activeIndex(), pv.members().size() - 1));
        return pv.members().get(i);
    }

    private void onClick(MouseEvent e) {
        CharacterView c = active();
        if (c == null) return;
        int memberIdx = controller.state().party().activeIndex();
        // equip slots column (x 20..76)
        if (e.getX() < 100) {
            EquipSlot[] slots = EquipSlot.values();
            int row = (e.getY() - 60) / (CELL + 6);
            if (row >= 0 && row < slots.length && c.equipped(slots[row]) != null) {
                controller.unequip(memberIdx, slots[row]);
            }
            return;
        }
        // backpack grid
        int gx = (e.getX() - 120) / (CELL + 6);
        int gy = (e.getY() - 60) / (CELL + 6);
        int idx = gy * COLS + gx;
        List<ItemView> inv = c.inventory();
        if (gx >= 0 && gx < COLS && idx >= 0 && idx < inv.size()) {
            ItemView it = inv.get(idx);
            if (it.slot() != null) controller.equip(memberIdx, it.id());
            else controller.useItem(memberIdx, it.id());
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        CharacterView c = active();
        g.setColor(new Color(232, 220, 160));
        g.setFont(UiUtil.UI_BOLD.deriveFont(18f));
        g.drawString("Inventory" + (c == null ? "" : " — " + c.name()) + "    (Esc / I to close)", 20, 34);
        if (c == null) return;

        // equipment column
        g.setFont(UiUtil.UI_FONT);
        EquipSlot[] slots = EquipSlot.values();
        for (int i = 0; i < slots.length; i++) {
            int y = 60 + i * (CELL + 6);
            g.setColor(new Color(40, 38, 46));
            g.fillRect(20, y, CELL, CELL);
            g.setColor(UiUtil.PANEL_EDGE);
            g.drawRect(20, y, CELL, CELL);
            ItemView it = c.equipped(slots[i]);
            if (it != null) SpriteFactory.drawItem(g, it.spriteKey(), 20, y, CELL, CELL);
            g.setColor(new Color(160, 150, 130));
            g.drawString(slots[i].name(), 20, y - 2);
        }

        // backpack grid
        List<ItemView> inv = c.inventory();
        for (int i = 0; i < inv.size(); i++) {
            int gx = i % COLS, gy = i / COLS;
            int x = 120 + gx * (CELL + 6), y = 60 + gy * (CELL + 6);
            g.setColor(new Color(38, 36, 44));
            g.fillRect(x, y, CELL, CELL);
            g.setColor(UiUtil.PANEL_EDGE);
            g.drawRect(x, y, CELL, CELL);
            ItemView it = inv.get(i);
            SpriteFactory.drawItem(g, it.spriteKey(), x, y, CELL, CELL);
            g.setColor(UiUtil.INK);
            g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 10));
            g.drawString(it.name(), x, y + CELL + 12);
            if (it.quantity() > 1) g.drawString("x" + it.quantity(), x + CELL - 16, y + 12);
        }
        g.setColor(new Color(200, 190, 160));
        g.setFont(UiUtil.UI_FONT);
        g.drawString("Gold: " + controller.state().gold(), 120, getHeight() - 20);
    }

    @Override public Dimension getPreferredSize() { return new Dimension(640, 560); }
}
