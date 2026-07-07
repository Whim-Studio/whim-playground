package com.whim.albion.items;

import com.whim.albion.api.ActionResult;
import com.whim.albion.api.Content;
import com.whim.albion.api.Defs.ItemDef;
import com.whim.albion.api.Enums.DamageType;
import com.whim.albion.api.Enums.EquipSlot;
import com.whim.albion.api.Enums.StatType;
import com.whim.albion.api.Views.ItemView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A character's carried items: a slot-limited backpack plus five equip slots.
 * Resolves equipment bonuses (attack / defense / stat modifiers) and holds the
 * logic for equipping, unequipping and consuming items. Item <em>effects</em>
 * that touch LP/SP (potions) are surfaced back to the owning
 * {@link com.whim.albion.entities.Character} which applies them.
 */
public final class Inventory {

    /** Default backpack capacity (distinct stacks). */
    public static final int DEFAULT_SLOTS = 24;

    private final Content content;
    private final int maxSlots;
    private final List<ItemStack> pack = new ArrayList<ItemStack>();
    private final Map<EquipSlot, ItemStack> equipped = new EnumMap<EquipSlot, ItemStack>(EquipSlot.class);

    public Inventory(Content content) {
        this(content, DEFAULT_SLOTS);
    }

    public Inventory(Content content, int maxSlots) {
        this.content = content;
        this.maxSlots = maxSlots;
    }

    // --------------------------------------------------------------- mutation

    /** Add {@code qty} of an item; stacks onto an existing pack stack when possible. */
    public boolean add(String itemId, int qty) {
        if (qty <= 0) return true;
        for (ItemStack s : pack) {
            if (s.itemId().equals(itemId)) { s.add(qty); return true; }
        }
        if (pack.size() >= maxSlots) return false;
        pack.add(new ItemStack(itemId, qty, content));
        return true;
    }

    /** Remove {@code qty} of an item from the pack; false if not enough present. */
    public boolean remove(String itemId, int qty) {
        ItemStack s = find(itemId);
        if (s == null || s.quantity() < qty) return false;
        s.remove(qty);
        if (s.quantity() <= 0) pack.remove(s);
        return true;
    }

    public boolean has(String itemId) {
        ItemStack s = find(itemId);
        return s != null && s.quantity() > 0;
    }

    private ItemStack find(String itemId) {
        for (ItemStack s : pack) {
            if (s.itemId().equals(itemId)) return s;
        }
        return null;
    }

    // ----------------------------------------------------------- equip/unequip

    public ActionResult equip(String itemId) {
        ItemStack s = find(itemId);
        if (s == null) return ActionResult.fail("Not carried.");
        ItemDef d = s.def();
        if (d == null || d.slot == null) return ActionResult.fail("Cannot be equipped.");
        // Return whatever currently occupies the slot to the pack.
        ItemStack current = equipped.remove(d.slot);
        if (current != null) add(current.itemId(), 1);
        equipped.put(d.slot, new ItemStack(itemId, 1, content));
        remove(itemId, 1);
        return ActionResult.ok("Equipped " + d.name + ".");
    }

    public ActionResult unequip(EquipSlot slot) {
        ItemStack current = equipped.remove(slot);
        if (current == null) return ActionResult.fail("Nothing equipped there.");
        add(current.itemId(), 1);
        ItemDef d = current.def();
        return ActionResult.ok("Removed " + (d == null ? current.itemId() : d.name) + ".");
    }

    public ItemStack equippedStack(EquipSlot slot) { return equipped.get(slot); }

    /** Pull a single consumable out of the pack; returns its def or null if absent. */
    public ItemDef takeConsumable(String itemId) {
        ItemStack s = find(itemId);
        if (s == null) return null;
        ItemDef d = s.def();
        if (d == null) return null;
        remove(itemId, 1);
        return d;
    }

    // --------------------------------------------------------------- readouts

    public int equipAttack() {
        ItemStack w = equipped.get(EquipSlot.WEAPON);
        return w == null || w.def() == null ? 0 : w.def().attack;
    }

    public DamageType weaponDamageType() {
        ItemStack w = equipped.get(EquipSlot.WEAPON);
        return w == null || w.def() == null ? DamageType.PHYSICAL : w.def().damageType;
    }

    public int equipDefense() {
        int total = 0;
        for (ItemStack s : equipped.values()) {
            if (s.def() != null) total += s.def().defense;
        }
        return total;
    }

    public int statBonus(StatType stat) {
        int total = 0;
        for (ItemStack s : equipped.values()) {
            if (s.def() == null) continue;
            Integer b = s.def().statBonuses.get(stat);
            if (b != null) total += b;
        }
        return total;
    }

    /** Backpack contents as read-only views (equipped items excluded). */
    public List<ItemView> asViews() {
        List<ItemView> out = new ArrayList<ItemView>(pack);
        return Collections.unmodifiableList(out);
    }

    public ItemView equippedView(EquipSlot slot) { return equipped.get(slot); }
}
