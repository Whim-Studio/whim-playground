package com.whim.necromunda.engine.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.whim.necromunda.model.Weapon;
import com.whim.necromunda.model.WeaponRule;

/**
 * The starter armoury: a fixed set of weapons keyed by a stable string id.
 * Rosters persist weapons <em>by id</em> and resolve them back through this
 * catalogue on load, so save files stay small and forward-compatible.
 *
 * <p>Profiles are functional reimplementations (range/S/damage/save-mod/flags),
 * not copied stat blocks. Archetype names are generic ("Auto Pistol",
 * "Las Gun", "Heavy Stubber") rather than trademarked product names.
 */
public final class WeaponCatalogue {

    private static final Map<String, Weapon> BY_ID = new LinkedHashMap<String, Weapon>();

    private WeaponCatalogue() {
    }

    private static void add(String id, String name, int rs, int rl, int s, int dmg,
                            int saveMod, int ammo, int cost, WeaponRule... rules) {
        Set<WeaponRule> set = EnumSet.noneOf(WeaponRule.class);
        Collections.addAll(set, rules);
        BY_ID.put(id, new Weapon(id, name, rs, rl, s, dmg, saveMod, ammo, cost, set));
    }

    static {
        // --- Pistols (short range, usable in close combat) ---
        add("AUTO_PISTOL",  "Auto Pistol",   6, 12, 3, 1, 0, 4, 10, WeaponRule.PISTOL, WeaponRule.AMMO_CHECK);
        add("LAS_PISTOL",   "Las Pistol",    8, 16, 3, 1, 0, 2, 15, WeaponRule.PISTOL);
        add("STUB_GUN",     "Stub Gun",      6, 12, 3, 1, 0, 4,  5, WeaponRule.PISTOL, WeaponRule.AMMO_CHECK);
        add("PLASMA_PISTOL","Plasma Pistol", 6, 12, 5, 1, 2, 6, 25, WeaponRule.PISTOL, WeaponRule.AMMO_CHECK);

        // --- Basic weapons (the workhorse guns) ---
        add("AUTOGUN",  "Autogun",  12, 24, 3, 1, 0, 4, 20, WeaponRule.RAPID_FIRE, WeaponRule.AMMO_CHECK);
        add("LASGUN",   "Las Gun",  12, 24, 3, 1, 0, 2, 25);
        add("SHOTGUN",  "Shotgun",   8, 16, 4, 1, 0, 4, 20, WeaponRule.KNOCKBACK, WeaponRule.AMMO_CHECK);

        // --- Special weapons (squad support) ---
        add("FLAMER",           "Flamer",           8,  8, 4, 1, 1, 5, 40, WeaponRule.TEMPLATE, WeaponRule.AMMO_CHECK);
        add("GRENADE_LAUNCHER", "Grenade Launcher", 12, 24, 4, 1, 1, 4, 40, WeaponRule.TEMPLATE, WeaponRule.AMMO_CHECK);
        add("PLASMA_GUN",       "Plasma Gun",       12, 24, 5, 1, 2, 6, 50, WeaponRule.AMMO_CHECK);

        // --- Heavy weapons (high S / long range, move-or-fire, Heavies only) ---
        add("HEAVY_STUBBER", "Heavy Stubber", 20, 40, 4, 1, 1, 4, 60, WeaponRule.RAPID_FIRE, WeaponRule.MOVE_OR_FIRE, WeaponRule.AMMO_CHECK);
        add("AUTOCANNON",    "Autocannon",    20, 72, 6, 2, 2, 4, 90, WeaponRule.MOVE_OR_FIRE, WeaponRule.AMMO_CHECK);

        // --- Close-combat weapons (melee only unless noted) ---
        add("KNIFE",        "Fighting Knife", 0, 0, 3, 1, 0, 0,  2, WeaponRule.MELEE);
        add("CLUB",         "Club / Maul",    0, 0, 4, 1, 0, 0,  5, WeaponRule.MELEE, WeaponRule.KNOCKBACK);
        add("SWORD",        "Sword",          0, 0, 3, 1, 0, 0, 10, WeaponRule.MELEE, WeaponRule.PARRY);
        add("POWER_SWORD",  "Power Sword",    0, 0, 4, 1, 2, 0, 30, WeaponRule.MELEE, WeaponRule.PARRY, WeaponRule.UNWIELDY);
    }

    public static Weapon byId(String id) {
        return BY_ID.get(id);
    }

    public static boolean contains(String id) {
        return BY_ID.containsKey(id);
    }

    /** All weapon ids in catalogue (insertion) order. */
    public static List<String> ids() {
        return new ArrayList<String>(BY_ID.keySet());
    }

    /** All weapons in catalogue order. */
    public static List<Weapon> all() {
        return new ArrayList<Weapon>(BY_ID.values());
    }
}
