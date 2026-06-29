package com.whim.ruinlander.engine;

import com.whim.ruinlander.domain.Inventory;
import com.whim.ruinlander.domain.Item;
import com.whim.ruinlander.domain.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Crafting / cooking. Recipes verify their inputs against an {@link Inventory}
 * before consuming them and producing the output. Deterministic; no RNG.
 */
public class CraftingSystem {

    private final List<Recipe> recipes = new ArrayList<Recipe>();

    public CraftingSystem() {
        recipes.add(new Recipe("cook_meat", "Cook Meat",
                inputs("raw_meat", 1), stack("cooked_meat", 1)));
        recipes.add(new Recipe("purify_water", "Purify Water",
                inputs("dirty_water", 1, "cloth", 1), stack("clean_water", 1)));
        recipes.add(new Recipe("make_bandage", "Craft Bandage",
                inputs("cloth", 2), stack("bandage", 1)));
        recipes.add(new Recipe("make_ammo", "Craft Ammo",
                inputs("scrap", 1, "gunpowder", 1), stack("ammo_9mm", 5)));
    }

    public List<Recipe> getRecipes() {
        return recipes;
    }

    /** True if {@code inv} holds every input in the required quantity. */
    public boolean canCraft(Inventory inv, Recipe r) {
        if (inv == null || r == null) {
            return false;
        }
        for (Map.Entry<String, Integer> e : r.getInputs().entrySet()) {
            if (inv.count(e.getKey()) < e.getValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Consume the recipe's inputs from {@code inv} and add the output. Returns the
     * produced {@link ItemStack}, or {@code null} if the inputs were insufficient.
     */
    public ItemStack craft(Inventory inv, Recipe r) {
        if (!canCraft(inv, r)) {
            return null;
        }
        for (Map.Entry<String, Integer> e : r.getInputs().entrySet()) {
            inv.remove(e.getKey(), e.getValue());
        }
        ItemStack out = r.getOutput();
        inv.add(out.getItem(), out.getQuantity());
        return out;
    }

    // ---- helpers -----------------------------------------------------------

    private static Map<String, Integer> inputs(Object... pairs) {
        Map<String, Integer> m = new LinkedHashMap<String, Integer>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            m.put((String) pairs[i], (Integer) pairs[i + 1]);
        }
        return m;
    }

    private static ItemStack stack(String id, int qty) {
        Item it = ItemDb.get(id);
        return new ItemStack(it, qty);
    }
}
