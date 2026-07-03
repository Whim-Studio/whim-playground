package com.arpg.model;

import java.util.HashMap;
import java.util.Map;

/**
 * A wearable {@link Item} occupying one {@link EquipmentSlot} and granting flat
 * {@link StatType} modifiers while equipped. Also tracks a reforge count so the
 * engine can implement re-rolling of stats (the model just stores the number).
 */
public final class Equipment extends Item {

    private static final long serialVersionUID = 1L;

    private final EquipmentSlot slot;
    private final int levelRequirement;
    private final Map<StatType, Integer> statModifiers;
    private int reforgeCount;

    public Equipment(String id, String name, String description, Rarity rarity, int vendorValue,
                     EquipmentSlot slot, int levelRequirement, Map<StatType, Integer> statModifiers) {
        super(id, name, description, rarity, vendorValue, false);
        if (slot == null) {
            throw new IllegalArgumentException("Equipment slot must not be null");
        }
        this.slot = slot;
        this.levelRequirement = Math.max(1, levelRequirement);
        this.statModifiers = new HashMap<StatType, Integer>();
        if (statModifiers != null) {
            this.statModifiers.putAll(statModifiers);
        }
        this.reforgeCount = 0;
    }

    public EquipmentSlot getSlot() {
        return slot;
    }

    public int getLevelRequirement() {
        return levelRequirement;
    }

    /** Copy of this item's flat stat modifiers. */
    public Map<StatType, Integer> getStatModifiers() {
        return new HashMap<StatType, Integer>(statModifiers);
    }

    public int getStatModifier(StatType type) {
        Integer v = statModifiers.get(type);
        return v == null ? 0 : v.intValue();
    }

    /**
     * Replace a modifier value in place (used by the engine when reforging).
     * Removing is done by passing 0. Kept here because it is pure data mutation.
     */
    public void setStatModifier(StatType type, int value) {
        if (type == null) {
            return;
        }
        if (value == 0) {
            statModifiers.remove(type);
        } else {
            statModifiers.put(type, Integer.valueOf(value));
        }
    }

    public int getReforgeCount() {
        return reforgeCount;
    }

    public void setReforgeCount(int reforgeCount) {
        this.reforgeCount = Math.max(0, reforgeCount);
    }

    public void incrementReforgeCount() {
        this.reforgeCount++;
    }

    @Override
    public String toString() {
        return getName() + " [" + rarity.getDisplayName() + " " + slot.getDisplayName()
                + ", req lvl " + levelRequirement + "]";
    }
}
