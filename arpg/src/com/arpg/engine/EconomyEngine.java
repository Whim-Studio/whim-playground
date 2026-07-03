package com.arpg.engine;

import com.arpg.model.Character;
import com.arpg.model.Equipment;
import com.arpg.model.Item;
import com.arpg.model.StatType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Currency management plus a risk/reward reforge gamble. The player pays gold and gambles on
 * rescaling an equipment's stat modifiers: reforging can improve, ruin, or shatter the gear.
 */
final class EconomyEngine {
    private final Random rng;
    private final EventBus bus;

    EconomyEngine(Random rng, EventBus bus) {
        this.rng = rng;
        this.bus = bus;
    }

    void addCurrency(Character player, int amount) {
        if (amount > 0) player.addGold(amount);
    }

    boolean spendCurrency(Character player, int amount) {
        if (amount <= 0) return true;
        return player.spendGold(amount);
    }

    int reforgeCost(Item item) {
        if (item == null) return 20;
        return Math.max(20, item.getVendorValue() * 2);
    }

    /**
     * Gamble gold to reforge an equipment's stats. Distribution (roll 0..99):
     * 0-14 shatter, 15-34 diminished, 35-49 unchanged, 50-89 improved, 90-99 masterwork.
     */
    ReforgeResult reforge(Character player, Item item) {
        if (item == null || !item.isEquipment()) {
            return new ReforgeResult(false, ReforgeResult.Outcome.UNCHANGED, item, 0,
                    "Only equipment can be reforged.");
        }
        int cost = reforgeCost(item);
        if (player.getGold() < cost) {
            return new ReforgeResult(false, ReforgeResult.Outcome.UNCHANGED, item, cost,
                    "Not enough gold to reforge (need " + cost + ").");
        }
        player.spendGold(cost);

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

        Equipment eq = (Equipment) item;
        if (factor != 1.0) {
            Map<StatType, Integer> current = new HashMap<StatType, Integer>(eq.getStatModifiers());
            for (Map.Entry<StatType, Integer> e : current.entrySet()) {
                int scaled = (int) Math.round(e.getValue() * factor);
                if (e.getValue() > 0 && scaled < 1) scaled = 1;
                eq.setStatModifier(e.getKey(), scaled);
            }
        }
        eq.incrementReforgeCount();
        String msg = eq.getName() + " reforged: " + outcome + " (x" + String.format("%.2f", factor)
                + " stats, -" + cost + " gold).";
        bus.log(msg);
        return new ReforgeResult(true, outcome, eq, cost, msg);
    }
}
