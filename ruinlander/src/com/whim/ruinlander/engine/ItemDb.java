package com.whim.ruinlander.engine;

import com.whim.ruinlander.domain.Armor;
import com.whim.ruinlander.domain.Item;
import com.whim.ruinlander.domain.ItemCategory;
import com.whim.ruinlander.domain.Weapon;
import com.whim.ruinlander.domain.WeaponClass;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static catalogue of every {@link Item}/{@link Weapon}/{@link Armor} definition in
 * Ruinlander. Pure data — no RNG, no Swing. {@link #all()} returns a fresh map of
 * canonical id -&gt; item that the controller feeds to {@code WorldFactory.build(map)}.
 */
public final class ItemDb {

    private static final Map<String, Item> DB = new LinkedHashMap<String, Item>();

    private ItemDb() {
    }

    static {
        // ---- Food (reduces HUNGER; cooked variants also heal) -------------
        // Item(id, name, category, weight, stackable, hunger, thirst, fatigue, health, radReduce)
        put(new Item("raw_meat", "Raw Meat", ItemCategory.FOOD, 0.5, true, 18, 0, 0, 0, 0));
        put(new Item("cooked_meat", "Cooked Meat", ItemCategory.FOOD, 0.5, true, 50, 0, 0, 6, 0));
        put(new Item("canned_food", "Canned Beans", ItemCategory.FOOD, 0.6, true, 40, 5, 0, 0, 0));
        put(new Item("ration", "Survival Ration", ItemCategory.FOOD, 0.4, true, 60, 0, 5, 0, 0));

        // ---- Water (reduces THIRST; dirty water carries radiation risk) ----
        put(new Item("dirty_water", "Dirty Water", ItemCategory.WATER, 1.0, true, 0, 28, 0, 0, -6));
        put(new Item("clean_water", "Purified Water", ItemCategory.WATER, 1.0, true, 0, 48, 0, 0, 0));

        // ---- Medical -------------------------------------------------------
        put(new Item("bandage", "Bandage", ItemCategory.MEDICAL, 0.2, true, 0, 0, 0, 28, 0));
        put(new Item("stimpak", "Stimpak", ItemCategory.MEDICAL, 0.3, true, 0, 0, 10, 55, 0));
        put(new Item("rad_away", "Rad-Away", ItemCategory.MEDICAL, 0.3, true, 0, 0, 0, 0, 55));

        // ---- Materials (crafting inputs) -----------------------------------
        put(new Item("scrap", "Scrap Metal", ItemCategory.MATERIAL, 0.8, true, 0, 0, 0, 0, 0));
        put(new Item("cloth", "Cloth", ItemCategory.MATERIAL, 0.2, true, 0, 0, 0, 0, 0));
        put(new Item("gunpowder", "Gunpowder", ItemCategory.MATERIAL, 0.3, true, 0, 0, 0, 0, 0));

        // ---- Ammo (MISC, consumed by firearms) -----------------------------
        put(new Item("ammo_9mm", "9mm Rounds", ItemCategory.MISC, 0.05, true, 0, 0, 0, 0, 0));

        // ---- Weapons -------------------------------------------------------
        // Weapon(id, name, weight, class, damage, accuracy, apCost, range, ammoItemId)
        put(new Weapon("pipe", "Lead Pipe", 2.5, WeaponClass.MELEE, 7, 0.78, 3, 1, null));
        put(new Weapon("machete", "Machete", 1.8, WeaponClass.MELEE, 12, 0.72, 3, 1, null));
        put(new Weapon("pistol", "9mm Pistol", 1.2, WeaponClass.FIREARM, 14, 0.70, 4, 6, "ammo_9mm"));

        // ---- Armor (id, name, weight, damageReduction, coverage) -----------
        put(new Armor("leather_jacket", "Leather Jacket", 3.0, 0.20, 0.6));
        put(new Armor("kevlar_vest", "Kevlar Vest", 5.0, 0.40, 0.8));
    }

    private static void put(Item i) {
        DB.put(i.getId(), i);
    }

    /** Look up any item by id, or {@code null} if unknown. */
    public static Item get(String id) {
        return DB.get(id);
    }

    /** Look up a {@link Weapon} by id, or {@code null} if not a weapon / unknown. */
    public static Weapon weapon(String id) {
        Item i = DB.get(id);
        return (i instanceof Weapon) ? (Weapon) i : null;
    }

    /** Look up an {@link Armor} by id, or {@code null} if not armor / unknown. */
    public static Armor armor(String id) {
        Item i = DB.get(id);
        return (i instanceof Armor) ? (Armor) i : null;
    }

    /** A fresh map of every definition (id -&gt; item). Safe for the caller to keep. */
    public static Map<String, Item> all() {
        return new LinkedHashMap<String, Item>(DB);
    }
}
