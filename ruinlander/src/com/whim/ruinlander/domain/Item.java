package com.whim.ruinlander.domain;

/**
 * An item definition. Instances are treated as immutable templates; quantity is
 * tracked separately on {@link ItemStack}. Effect fields describe how the engine
 * should adjust survival stats when the item is consumed (positive value = the
 * magnitude of the beneficial change; the engine decides direction per stat).
 */
public class Item {
    private final String id;
    private final String name;
    private final ItemCategory category;
    private final double weight;
    private final boolean stackable;

    // Consumable effect magnitudes (0 if not applicable).
    private final int hungerRestore;   // lowers HUNGER
    private final int thirstRestore;   // lowers THIRST
    private final int fatigueRestore;  // lowers FATIGUE
    private final int healthRestore;   // raises HEALTH
    private final int radiationReduce; // lowers RADIATION

    public Item(String id, String name, ItemCategory category, double weight, boolean stackable) {
        this(id, name, category, weight, stackable, 0, 0, 0, 0, 0);
    }

    public Item(String id, String name, ItemCategory category, double weight, boolean stackable,
                int hungerRestore, int thirstRestore, int fatigueRestore,
                int healthRestore, int radiationReduce) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.weight = weight;
        this.stackable = stackable;
        this.hungerRestore = hungerRestore;
        this.thirstRestore = thirstRestore;
        this.fatigueRestore = fatigueRestore;
        this.healthRestore = healthRestore;
        this.radiationReduce = radiationReduce;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public ItemCategory getCategory() { return category; }
    public double getWeight() { return weight; }
    public boolean isStackable() { return stackable; }

    public int getHungerRestore() { return hungerRestore; }
    public int getThirstRestore() { return thirstRestore; }
    public int getFatigueRestore() { return fatigueRestore; }
    public int getHealthRestore() { return healthRestore; }
    public int getRadiationReduce() { return radiationReduce; }

    /** True if consuming this item changes any survival stat. */
    public boolean isConsumable() {
        return hungerRestore != 0 || thirstRestore != 0 || fatigueRestore != 0
                || healthRestore != 0 || radiationReduce != 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Item)) return false;
        return id.equals(((Item) obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
