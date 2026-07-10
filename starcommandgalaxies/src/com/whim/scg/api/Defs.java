package com.whim.scg.api;

import java.util.Collections;
import java.util.List;

/**
 * Immutable content templates. The engine's content loader (Task 1) builds these
 * from the JSON files under {@code data/}; other tasks may read them but never
 * construct gameplay from raw JSON themselves.
 */
public final class Defs {
    private Defs() {}

    public static final class WeaponDef {
        public final String id, name;
        public final Enums.WeaponType type;
        public final int damage, chargeTicks, power, cost;
        public final boolean piercesShields; // ion / torpedo behaviour
        public WeaponDef(String id, String name, Enums.WeaponType type, int damage,
                         int chargeTicks, int power, int cost, boolean piercesShields) {
            this.id = id; this.name = name; this.type = type; this.damage = damage;
            this.chargeTicks = chargeTicks; this.power = power; this.cost = cost;
            this.piercesShields = piercesShields;
        }
    }

    public static final class RoomDef {
        public final Enums.RoomType type;
        public final int w, h, maxPower, maxHp;
        public RoomDef(Enums.RoomType type, int w, int h, int maxPower, int maxHp) {
            this.type = type; this.w = w; this.h = h; this.maxPower = maxPower; this.maxHp = maxHp;
        }
    }

    public static final class ShipDef {
        public final String id, name;
        public final Enums.Faction faction;
        public final int gridW, gridH, maxHull, reactor, cost;
        public final List<RoomDef> rooms;
        public final List<String> weaponIds;
        public ShipDef(String id, String name, Enums.Faction faction, int gridW, int gridH,
                       int maxHull, int reactor, int cost, List<RoomDef> rooms, List<String> weaponIds) {
            this.id = id; this.name = name; this.faction = faction; this.gridW = gridW;
            this.gridH = gridH; this.maxHull = maxHull; this.reactor = reactor; this.cost = cost;
            this.rooms = ro(rooms); this.weaponIds = ro(weaponIds);
        }
    }

    public static final class RoleDef {
        public final Enums.CrewRole role;
        public final String title;
        public final int baseHp;
        public RoleDef(Enums.CrewRole role, String title, int baseHp) {
            this.role = role; this.title = title; this.baseHp = baseHp;
        }
    }

    public static final class TechDef {
        public final Enums.TechType type;
        public final String name;
        public final int maxLevel, baseCost;
        public TechDef(Enums.TechType type, String name, int maxLevel, int baseCost) {
            this.type = type; this.name = name; this.maxLevel = maxLevel; this.baseCost = baseCost;
        }
    }

    private static <T> List<T> ro(List<T> in) {
        return in == null ? Collections.<T>emptyList() : Collections.unmodifiableList(in);
    }
}
