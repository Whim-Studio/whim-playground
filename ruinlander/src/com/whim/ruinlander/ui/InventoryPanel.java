package com.whim.ruinlander.ui;

import com.whim.ruinlander.domain.GameMode;
import com.whim.ruinlander.domain.ItemStack;
import com.whim.ruinlander.engine.CraftingSystem;
import com.whim.ruinlander.engine.Recipe;

import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

/**
 * Left column: the player's inventory grid plus the crafting menu. Number keys
 * 1-9 act on the listed rows (use/equip in INVENTORY mode, craft in CRAFTING
 * mode) — handled by {@link GameController}.
 */
public class InventoryPanel extends JTextArea {

    private final GameController controller;

    public InventoryPanel(GameController controller) {
        this.controller = controller;
        setEditable(false);
        setBackground(new Color(20, 19, 17));
        setForeground(new Color(200, 200, 180));
        setFont(new Font("Monospaced", Font.PLAIN, 12));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setPreferredSize(new Dimension(240, 480));
        refresh();
    }

    /** Rebuild the inventory + crafting text from current state. Call on the EDT. */
    public void refresh() {
        StringBuilder sb = new StringBuilder();
        GameMode mode = controller.getStateManager().getMode();

        sb.append("== INVENTORY ==");
        sb.append(mode == GameMode.INVENTORY ? "  [active: 1-9 use/equip]\n" : "  [I]\n");
        List<ItemStack> stacks = controller.getStateManager().getPlayer().getInventory().getStacks();
        if (stacks.isEmpty()) {
            sb.append("  (empty)\n");
        } else {
            for (int i = 0; i < stacks.size(); i++) {
                ItemStack s = stacks.get(i);
                String idx = (i < 9) ? (i + 1) + ")" : "  ";
                sb.append(" ").append(idx).append(" ")
                        .append(s.getItem().getName())
                        .append(" x").append(s.getQuantity()).append("\n");
            }
        }

        sb.append("\n== CRAFTING ==");
        sb.append(mode == GameMode.CRAFTING ? "  [active: 1-9 craft]\n" : "  [C]\n");
        CraftingSystem crafting = controller.getCrafting();
        List<Recipe> recipes = crafting.getRecipes();
        for (int i = 0; i < recipes.size(); i++) {
            Recipe r = recipes.get(i);
            boolean ok = crafting.canCraft(controller.getStateManager().getPlayer().getInventory(), r);
            sb.append(" ").append(i + 1).append(") ")
                    .append(ok ? "+ " : "x ")
                    .append(r.describe()).append("\n");
        }

        setText(sb.toString());
        setCaretPosition(0);
    }
}
