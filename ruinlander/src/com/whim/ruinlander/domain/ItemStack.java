package com.whim.ruinlander.domain;

/** A quantity of a single {@link Item}. */
public class ItemStack {
    private final Item item;
    private int quantity;

    public ItemStack(Item item, int quantity) {
        if (item == null) throw new IllegalArgumentException("item must not be null");
        this.item = item;
        this.quantity = Math.max(0, quantity);
    }

    public Item getItem() { return item; }
    public int getQuantity() { return quantity; }

    public void setQuantity(int q) { this.quantity = Math.max(0, q); }
    public void add(int q) { this.quantity = Math.max(0, this.quantity + q); }

    /** Total weight contributed by this stack. */
    public double totalWeight() {
        return item.getWeight() * quantity;
    }

    public boolean isEmpty() {
        return quantity <= 0;
    }

    @Override
    public String toString() {
        return item.getName() + " x" + quantity;
    }
}
