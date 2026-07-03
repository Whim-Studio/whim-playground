package com.arpg.engine;

import com.arpg.model.Item;
import com.arpg.model.LootTable;

import java.util.Random;

/**
 * Rolls loot from a {@link LootTable} using the table's built-in rarity/entry weighting.
 * Pure RNG math — returns the rolled {@link Item} (shared content instance) or {@code null} on no drop.
 */
final class LootEngine {
    private static final int DROP_CHANCE_PERCENT = 70;

    private final Random rng;

    LootEngine(Random rng) {
        this.rng = rng;
    }

    Item rollLoot(LootTable table, int playerLevel) {
        if (table == null || table.isEmpty()) return null;
        if (rng.nextInt(100) >= DROP_CHANCE_PERCENT) return null;
        int total = table.getTotalWeight();
        if (total <= 0) return null;
        return table.pick(rng.nextInt(total));
    }

    /** Roll the gold reward band for a table (0 when none configured). */
    int rollGold(LootTable table) {
        if (table == null) return 0;
        int min = table.getGoldMin();
        int max = table.getGoldMax();
        if (max <= min) return Math.max(0, min);
        return min + rng.nextInt(max - min + 1);
    }
}
