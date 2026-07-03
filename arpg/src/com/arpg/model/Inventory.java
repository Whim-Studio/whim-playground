package com.arpg.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple bag of {@link Item}s with a capacity limit. No stacking logic beyond
 * counting duplicates on demand — the engine decides what to do with contents.
 */
public final class Inventory implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private final List<Item> items;
    private int capacity;

    public Inventory() {
        this(40);
    }

    public Inventory(int capacity) {
        this.items = new ArrayList<Item>();
        this.capacity = Math.max(1, capacity);
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = Math.max(1, capacity);
    }

    public int size() {
        return items.size();
    }

    public boolean isFull() {
        return items.size() >= capacity;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    /** Add an item if there is room. Returns true if it was stored. */
    public boolean add(Item item) {
        if (item == null || isFull()) {
            return false;
        }
        return items.add(item);
    }

    public boolean remove(Item item) {
        return items.remove(item);
    }

    public Item findById(String itemId) {
        if (itemId == null) {
            return null;
        }
        for (int i = 0; i < items.size(); i++) {
            if (itemId.equals(items.get(i).getId())) {
                return items.get(i);
            }
        }
        return null;
    }

    public boolean contains(Item item) {
        return items.contains(item);
    }

    public List<Item> getItems() {
        return new ArrayList<Item>(items);
    }

    /** Only the equippable items, in insertion order. */
    public List<Equipment> getEquipment() {
        List<Equipment> out = new ArrayList<Equipment>();
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            if (it instanceof Equipment) {
                out.add((Equipment) it);
            }
        }
        return out;
    }

    public void clear() {
        items.clear();
    }
}
