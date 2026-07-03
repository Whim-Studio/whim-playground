package com.whim.cardwoven.domain;

import java.util.EnumMap;
import java.util.Map;

import com.whim.cardwoven.api.Enums.ResourceType;

/**
 * A player's resource purse: GOLD and COMMAND_POINTS. Never goes negative.
 */
public final class Resources {

    private final Map<ResourceType, Integer> amounts =
            new EnumMap<ResourceType, Integer>(ResourceType.class);

    public Resources() {
        this(0, 0);
    }

    public Resources(int gold, int command) {
        amounts.put(ResourceType.GOLD, gold);
        amounts.put(ResourceType.COMMAND_POINTS, command);
    }

    /** Current amount of a resource. */
    public int get(ResourceType type) {
        Integer v = amounts.get(type);
        return v == null ? 0 : v.intValue();
    }

    /** Add (positive) to a resource; clamps at zero if handed a negative delta. */
    public void add(ResourceType type, int delta) {
        int next = get(type) + delta;
        amounts.put(type, next < 0 ? 0 : next);
    }

    /** Whether the purse holds at least {@code amount} of a resource. */
    public boolean canAfford(ResourceType type, int amount) {
        return get(type) >= amount;
    }

    /**
     * Spend {@code amount} of a resource. Returns false and does nothing if the
     * purse cannot afford it.
     */
    public boolean spend(ResourceType type, int amount) {
        if (amount < 0 || !canAfford(type, amount)) {
            return false;
        }
        amounts.put(type, get(type) - amount);
        return true;
    }

    @Override
    public String toString() {
        return "Resources(gold=" + get(ResourceType.GOLD)
                + ",command=" + get(ResourceType.COMMAND_POINTS) + ")";
    }
}
