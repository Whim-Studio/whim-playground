package com.arpg.engine;

import com.arpg.model.Character;
import com.arpg.model.Item;

import java.util.Random;

/**
 * Currency management plus a risk/reward reforge gamble. Pure math/RNG: the caller pays the cost,
 * swaps the item and reports the {@link ReforgeResult}. Reforging can improve, ruin, or shatter gear.
 */
final class EconomyEngine {
    private final Random rng;
    private final EventBus bus;

    EconomyEngine(Random rng, EventBus bus) {
        this.rng = rng;
        this.bus = bus;
    }

    void addCurrency(Character player, long amount) {
        if (amount <= 0) return;
        player.setCurrency(player.getCurrency() + amount);
    }

    boolean spendCurrency(Character player, long amount) {
        if (amount <= 0) return true;
        if (player.getCurrency() < amount) return false;
        player.setCurrency(player.getCurrency() - amount);
        return true;
    }

    int reforgeCost(Item item) {
        return Math.max(20, item.getSellValue() * 2);
    }

    /**
     * Gamble currency to reforge an item's stats. The player must already hold enough currency;
     * this method only computes and reports the outcome. Distribution (roll 0..99):
     * 0-14 shatter, 15-34 diminished, 35-49 unchanged, 50-89 improved, 90-99 masterwork.
     */
    ReforgeResult reforge(Character player, Item item) {
        int cost = reforgeCost(item);
        if (item == null || !item.isEquipment()) {
            return new ReforgeResult(false, ReforgeResult.Outcome.UNCHANGED, item, 0,
                    "Only equipment can be reforged.");
        }
        if (player.getCurrency() < cost) {
            return new ReforgeResult(false, ReforgeResult.Outcome.UNCHANGED, item, cost,
                    "Not enough gold to reforge (need " + cost + ").");
        }
        player.setCurrency(player.getCurrency() - cost);

        int roll = rng.nextInt(100);
        ReforgeResult.Outcome outcome;
        double factor;
        if (roll < 15) { outcome = ReforgeResult.Outcome.SHATTERED; factor = 0.0; }
        else if (roll < 35) { outcome = ReforgeResult.Outcome.DIMINISHED; factor = 0.7; }
        else if (roll < 50) { outcome = ReforgeResult.Outcome.UNCHANGED; factor = 1.0; }
        else if (roll < 90) { outcome = ReforgeResult.Outcome.IMPROVED; factor = 1.25 + rng.nextInt(21) / 100.0; }
        else { outcome = ReforgeResult.Outcome.MASTERWORK; factor = 1.6 + rng.nextInt(41) / 100.0; }

        if (outcome == ReforgeResult.Outcome.SHATTERED) {
            String msg = item.getName() + " shattered in the forge! (-" + cost + " gold)";
            bus.log(msg);
            return new ReforgeResult(true, outcome, null, cost, msg);
        }

        int newSell = (int) Math.round(item.getSellValue() * Math.max(0.5, factor));
        Item reforged = item.withScaledStats(factor, newSell);
        String msg = item.getName() + " reforged: " + outcome + " (x" + String.format("%.2f", factor)
                + " stats, -" + cost + " gold).";
        bus.log(msg);
        return new ReforgeResult(true, outcome, reforged, cost, msg);
    }
}
