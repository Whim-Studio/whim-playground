package com.arpg.model;

/**
 * Base class for anything that can sit in an {@link Inventory}: consumables,
 * crafting materials, or (via the {@link Equipment} subclass) wearable gear.
 */
public class Item implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    protected final String id;
    protected final String name;
    protected final String description;
    protected final Rarity rarity;
    protected final int vendorValue;
    protected final boolean stackable;

    public Item(String id, String name, String description, Rarity rarity, int vendorValue, boolean stackable) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Item id must not be blank");
        }
        this.id = id;
        this.name = name;
        this.description = description;
        this.rarity = rarity == null ? Rarity.COMMON : rarity;
        this.vendorValue = Math.max(0, vendorValue);
        this.stackable = stackable;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public int getVendorValue() {
        return vendorValue;
    }

    public boolean isStackable() {
        return stackable;
    }

    public boolean isEquipment() {
        return this instanceof Equipment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Item)) {
            return false;
        }
        return id.equals(((Item) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return name + " [" + rarity.getDisplayName() + "]";
    }
}
