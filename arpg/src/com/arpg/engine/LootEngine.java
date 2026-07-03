package com.arpg.engine;

import com.arpg.model.Item;
import com.arpg.model.LootTable;
import com.arpg.model.Rarity;

import java.util.List;
import java.util.Random;

/**
 * Rolls loot from a {@link LootTable} with rarity/entry weighting and applies level-appropriate
 * stat scaling. Pure RNG math — returns a fresh scaled {@link Item} or {@code null} on no drop.
 */
final class LootEngine {
    private final Random rng;

    LootEngine(Random rng) {
        this.rng = rng;
    }

    Item rollLoot(LootTable table, int playerLevel) {
        if (table == null) return null;
        if (rng.nextInt(100) >= table.getDropChancePercent()) return null;

        List<LootTable.Entry> entries = table.getEntries();
        if (entries.isEmpty()) return null;

        int totalWeight = 0;
        for (int i = 0; i < entries.size(); i++) {
            totalWeight += weightOf(entries.get(i));
        }
        if (totalWeight <= 0) return null;

        int roll = rng.nextInt(totalWeight);
        Item chosen = null;
        for (int i = 0; i < entries.size(); i++) {
            roll -= weightOf(entries.get(i));
            if (roll < 0) { chosen = entries.get(i).getItem(); break; }
        }
        if (chosen == null) chosen = entries.get(entries.size() - 1).getItem();

        return scaleForLevel(chosen, playerLevel);
    }

    /** Entry weight biased by the item's rarity weight so legendaries stay scarce. */
    private static int weightOf(LootTable.Entry entry) {
        Rarity r = entry.getItem().getRarity();
        int rarityWeight = r == null ? 100 : r.getWeight();
        return Math.max(1, entry.getWeight() * rarityWeight / 100);
    }

    private static Item scaleForLevel(Item base, int playerLevel) {
        double factor = 1.0 + 0.06 * Math.max(0, playerLevel - 1);
        int sell = (int) Math.round(base.getSellValue() * factor);
        return base.withScaledStats(factor, sell);
    }
}
