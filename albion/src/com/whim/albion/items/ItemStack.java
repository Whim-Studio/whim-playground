package com.whim.albion.items;

import com.whim.albion.api.Content;
import com.whim.albion.api.Defs.ItemDef;
import com.whim.albion.api.Enums.EquipSlot;
import com.whim.albion.api.Enums.ItemType;
import com.whim.albion.api.Views.ItemView;

/**
 * A stack of one item id with a quantity. Also serves as its own read-only
 * {@link ItemView} for the UI by resolving its {@link ItemDef} from {@link Content}.
 */
public final class ItemStack implements ItemView {

    private final String itemId;
    private int quantity;
    private final Content content;

    public ItemStack(String itemId, int quantity, Content content) {
        this.itemId = itemId;
        this.quantity = Math.max(0, quantity);
        this.content = content;
    }

    public String itemId() { return itemId; }

    public void add(int n) { quantity += n; }

    /** Remove up to {@code n}; returns the amount actually removed. */
    public int remove(int n) {
        int removed = Math.min(n, quantity);
        quantity -= removed;
        return removed;
    }

    public ItemDef def() { return content.item(itemId); }

    // ---------------------------------------------------------------- ItemView

    @Override public String id() { return itemId; }

    @Override public String name() {
        ItemDef d = def();
        return d == null ? itemId : d.name;
    }

    @Override public ItemType type() {
        ItemDef d = def();
        return d == null ? ItemType.MISC : d.type;
    }

    @Override public EquipSlot slot() {
        ItemDef d = def();
        return d == null ? null : d.slot;
    }

    @Override public int quantity() { return quantity; }

    @Override public int value() {
        ItemDef d = def();
        return d == null ? 0 : d.value;
    }

    @Override public String description() {
        ItemDef d = def();
        return d == null ? "" : d.description;
    }

    @Override public String spriteKey() {
        ItemDef d = def();
        return d == null ? "" : d.spriteKey;
    }
}
