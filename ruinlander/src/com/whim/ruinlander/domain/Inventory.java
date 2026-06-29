package com.whim.ruinlander.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A weight-bounded collection of {@link ItemStack}s. */
public class Inventory {
    private final List<ItemStack> stacks = new ArrayList<ItemStack>();
    private final double capacity; // max total weight

    public Inventory() {
        this(50.0);
    }

    public Inventory(double capacity) {
        this.capacity = capacity;
    }

    public double getCapacity() { return capacity; }

    public List<ItemStack> getStacks() {
        return Collections.unmodifiableList(stacks);
    }

    /** Current total weight of all stacks. */
    public double currentWeight() {
        double w = 0.0;
        for (int i = 0; i < stacks.size(); i++) {
            w += stacks.get(i).totalWeight();
        }
        return w;
    }

    public double remainingCapacity() {
        return capacity - currentWeight();
    }

    public boolean canFit(Item item, int qty) {
        return item.getWeight() * qty <= remainingCapacity() + 1e-9;
    }

    /**
     * Add up to {@code qty} of an item, respecting weight capacity. Stackable
     * items merge into an existing stack. Returns the quantity actually added.
     */
    public int add(Item item, int qty) {
        if (item == null || qty <= 0) return 0;
        int canAddByWeight = item.getWeight() <= 0
                ? qty
                : (int) Math.floor(remainingCapacity() / item.getWeight() + 1e-9);
        int toAdd = Math.min(qty, canAddByWeight);
        if (toAdd <= 0) return 0;

        if (item.isStackable()) {
            ItemStack existing = find(item.getId());
            if (existing != null) {
                existing.add(toAdd);
                return toAdd;
            }
            stacks.add(new ItemStack(item, toAdd));
            return toAdd;
        }
        // Non-stackable: one stack per unit.
        for (int i = 0; i < toAdd; i++) {
            stacks.add(new ItemStack(item, 1));
        }
        return toAdd;
    }

    public int add(ItemStack stack) {
        if (stack == null) return 0;
        return add(stack.getItem(), stack.getQuantity());
    }

    /** Remove up to {@code qty} of an item by id. Returns the quantity removed. */
    public int remove(String itemId, int qty) {
        if (itemId == null || qty <= 0) return 0;
        int remaining = qty;
        for (int i = stacks.size() - 1; i >= 0 && remaining > 0; i--) {
            ItemStack s = stacks.get(i);
            if (s.getItem().getId().equals(itemId)) {
                int take = Math.min(remaining, s.getQuantity());
                s.add(-take);
                remaining -= take;
                if (s.isEmpty()) {
                    stacks.remove(i);
                }
            }
        }
        return qty - remaining;
    }

    /** Total quantity of an item across all stacks. */
    public int count(String itemId) {
        int c = 0;
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack s = stacks.get(i);
            if (s.getItem().getId().equals(itemId)) {
                c += s.getQuantity();
            }
        }
        return c;
    }

    /** First stack matching the item id, or null. */
    public ItemStack find(String itemId) {
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack s = stacks.get(i);
            if (s.getItem().getId().equals(itemId)) {
                return s;
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return stacks.isEmpty();
    }

    public int distinctCount() {
        return stacks.size();
    }
}
