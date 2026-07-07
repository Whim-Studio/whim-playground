package com.whim.albion.api;

import com.whim.albion.api.Enums.DamageType;
import com.whim.albion.api.Enums.EnemyBehaviorType;
import com.whim.albion.api.Enums.EquipSlot;
import com.whim.albion.api.Enums.ItemType;
import com.whim.albion.api.Enums.MapType;
import com.whim.albion.api.Enums.SpellEffectType;
import com.whim.albion.api.Enums.SpellSchool;
import com.whim.albion.api.Enums.StatType;
import com.whim.albion.api.Enums.TargetType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable content definitions shared across packages. These are the data-driven
 * "templates" loaded by the data package and consumed by the entities/combat/magic
 * engine. Instances are created via builders to stay Java-8 friendly.
 */
public final class Defs {

    private Defs() {}

    // ------------------------------------------------------------------ items

    /** Template for an item (weapon, armor, consumable, scroll, key, ...). */
    public static final class ItemDef {
        public final String id;
        public final String name;
        public final ItemType type;
        public final EquipSlot slot;      // null if not equippable
        public final int value;           // gold value
        public final int weight;          // arbitrary units
        public final int attack;          // weapon damage bonus
        public final int defense;         // armor/shield defense bonus
        public final DamageType damageType;
        public final Map<StatType, Integer> statBonuses;
        public final String spellId;      // scroll -> spell it teaches/casts, else null
        public final int healAmount;      // consumable LP restore
        public final int manaAmount;      // consumable SP restore
        public final String description;
        public final String spriteKey;    // procedural-art lookup key

        private ItemDef(Builder b) {
            this.id = b.id; this.name = b.name; this.type = b.type; this.slot = b.slot;
            this.value = b.value; this.weight = b.weight; this.attack = b.attack;
            this.defense = b.defense; this.damageType = b.damageType;
            this.statBonuses = Collections.unmodifiableMap(b.statBonuses);
            this.spellId = b.spellId; this.healAmount = b.healAmount;
            this.manaAmount = b.manaAmount; this.description = b.description;
            this.spriteKey = b.spriteKey;
        }

        public static Builder builder(String id, String name, ItemType type) {
            return new Builder(id, name, type);
        }

        public static final class Builder {
            private final String id;
            private final String name;
            private final ItemType type;
            private EquipSlot slot;
            private int value;
            private int weight;
            private int attack;
            private int defense;
            private DamageType damageType = DamageType.PHYSICAL;
            private final Map<StatType, Integer> statBonuses = new EnumMap<StatType, Integer>(StatType.class);
            private String spellId;
            private int healAmount;
            private int manaAmount;
            private String description = "";
            private String spriteKey = "";

            private Builder(String id, String name, ItemType type) {
                this.id = id; this.name = name; this.type = type;
            }

            public Builder slot(EquipSlot s) { this.slot = s; return this; }
            public Builder value(int v) { this.value = v; return this; }
            public Builder weight(int w) { this.weight = w; return this; }
            public Builder attack(int a) { this.attack = a; return this; }
            public Builder defense(int d) { this.defense = d; return this; }
            public Builder damageType(DamageType d) { this.damageType = d; return this; }
            public Builder statBonus(StatType s, int amt) { this.statBonuses.put(s, amt); return this; }
            public Builder spellId(String s) { this.spellId = s; return this; }
            public Builder heal(int h) { this.healAmount = h; return this; }
            public Builder mana(int m) { this.manaAmount = m; return this; }
            public Builder description(String d) { this.description = d; return this; }
            public Builder sprite(String s) { this.spriteKey = s; return this; }
            public ItemDef build() { return new ItemDef(this); }
        }
    }

    // ----------------------------------------------------------------- spells

    /** Template for a spell. */
    public static final class SpellDef {
        public final String id;
        public final String name;
        public final SpellSchool school;
        public final SpellEffectType effect;
        public final TargetType target;
        public final DamageType damageType;
        public final int spCost;
        public final int magnitude;       // damage / heal / buff amount
        public final int levelReq;
        public final int talentReq;       // MAGIC_TALENT required
        public final String description;

        private SpellDef(Builder b) {
            this.id = b.id; this.name = b.name; this.school = b.school; this.effect = b.effect;
            this.target = b.target; this.damageType = b.damageType; this.spCost = b.spCost;
            this.magnitude = b.magnitude; this.levelReq = b.levelReq; this.talentReq = b.talentReq;
            this.description = b.description;
        }

        public static Builder builder(String id, String name, SpellSchool school) {
            return new Builder(id, name, school);
        }

        public static final class Builder {
            private final String id;
            private final String name;
            private final SpellSchool school;
            private SpellEffectType effect = SpellEffectType.DAMAGE;
            private TargetType target = TargetType.SINGLE_ENEMY;
            private DamageType damageType = DamageType.MAGIC;
            private int spCost = 1;
            private int magnitude;
            private int levelReq = 1;
            private int talentReq;
            private String description = "";

            private Builder(String id, String name, SpellSchool school) {
                this.id = id; this.name = name; this.school = school;
            }

            public Builder effect(SpellEffectType e) { this.effect = e; return this; }
            public Builder target(TargetType t) { this.target = t; return this; }
            public Builder damageType(DamageType d) { this.damageType = d; return this; }
            public Builder spCost(int c) { this.spCost = c; return this; }
            public Builder magnitude(int m) { this.magnitude = m; return this; }
            public Builder levelReq(int l) { this.levelReq = l; return this; }
            public Builder talentReq(int t) { this.talentReq = t; return this; }
            public Builder description(String d) { this.description = d; return this; }
            public SpellDef build() { return new SpellDef(this); }
        }
    }

    // --------------------------------------------------------------- monsters

    /** Template for an enemy combatant. */
    public static final class MonsterDef {
        public final String id;
        public final String name;
        public final EnemyBehaviorType behavior;
        public final Map<StatType, Integer> stats;
        public final int maxLp;
        public final int maxSp;
        public final int attack;
        public final int defense;
        public final DamageType damageType;
        public final int xpReward;
        public final int goldReward;
        public final String spriteKey;

        private MonsterDef(Builder b) {
            this.id = b.id; this.name = b.name; this.behavior = b.behavior;
            this.stats = Collections.unmodifiableMap(b.stats); this.maxLp = b.maxLp;
            this.maxSp = b.maxSp; this.attack = b.attack; this.defense = b.defense;
            this.damageType = b.damageType; this.xpReward = b.xpReward;
            this.goldReward = b.goldReward; this.spriteKey = b.spriteKey;
        }

        public static Builder builder(String id, String name) { return new Builder(id, name); }

        public static final class Builder {
            private final String id;
            private final String name;
            private EnemyBehaviorType behavior = EnemyBehaviorType.AGGRESSIVE;
            private final Map<StatType, Integer> stats = new EnumMap<StatType, Integer>(StatType.class);
            private int maxLp = 10;
            private int maxSp = 0;
            private int attack = 3;
            private int defense = 0;
            private DamageType damageType = DamageType.PHYSICAL;
            private int xpReward = 5;
            private int goldReward = 0;
            private String spriteKey = "";

            private Builder(String id, String name) { this.id = id; this.name = name; }

            public Builder behavior(EnemyBehaviorType b) { this.behavior = b; return this; }
            public Builder stat(StatType s, int v) { this.stats.put(s, v); return this; }
            public Builder maxLp(int v) { this.maxLp = v; return this; }
            public Builder maxSp(int v) { this.maxSp = v; return this; }
            public Builder attack(int v) { this.attack = v; return this; }
            public Builder defense(int v) { this.defense = v; return this; }
            public Builder damageType(DamageType d) { this.damageType = d; return this; }
            public Builder xp(int v) { this.xpReward = v; return this; }
            public Builder gold(int v) { this.goldReward = v; return this; }
            public Builder sprite(String s) { this.spriteKey = s; return this; }
            public MonsterDef build() { return new MonsterDef(this); }
        }
    }

    // ------------------------------------------------------------------- maps

    /** Lightweight map metadata (full grid lives in the world package). */
    public static final class MapMeta {
        public final String id;
        public final String name;
        public final MapType type;
        public final int width;
        public final int height;

        public MapMeta(String id, String name, MapType type, int width, int height) {
            this.id = id; this.name = name; this.type = type;
            this.width = width; this.height = height;
        }
    }
}
