package com.whim.alganon.api;

import com.whim.alganon.api.Enums.AbilityKind;
import com.whim.alganon.api.Enums.ClassId;
import com.whim.alganon.api.Enums.DamageType;
import com.whim.alganon.api.Enums.EquipSlot;
import com.whim.alganon.api.Enums.Faction;
import com.whim.alganon.api.Enums.FamilyArchetype;
import com.whim.alganon.api.Enums.ItemType;
import com.whim.alganon.api.Enums.MobBehavior;
import com.whim.alganon.api.Enums.ObjectiveType;
import com.whim.alganon.api.Enums.ResourceType;
import com.whim.alganon.api.Enums.School;
import com.whim.alganon.api.Enums.SkillType;
import com.whim.alganon.api.Enums.StatType;
import com.whim.alganon.api.Enums.TargetType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable content templates authored by Task 1 (Model &amp; Content) and consumed
 * everywhere. All flavor text is authored fresh (clean-room). Simple fluent builders
 * keep content files readable. Java 8 only.
 */
public final class Defs {
    private Defs() {}

    /** Playable race, tied to a faction. */
    public static final class RaceDef {
        public final String id, name, description;
        public final Faction faction;
        public final Map<StatType, Integer> statMods;   // additive starting-stat tweaks
        public RaceDef(String id, String name, Faction faction, Map<StatType, Integer> statMods, String description) {
            this.id = id; this.name = name; this.faction = faction;
            this.statMods = Collections.unmodifiableMap(new EnumMap<StatType, Integer>(statMods));
            this.description = description;
        }
    }

    /** One of a race's five families. Archetype drives passive bonus + quest bias. */
    public static final class FamilyDef {
        public final String id, name, description;
        public final Faction faction;
        public final FamilyArchetype archetype;
        public FamilyDef(String id, String name, Faction faction, FamilyArchetype archetype, String description) {
            this.id = id; this.name = name; this.faction = faction;
            this.archetype = archetype; this.description = description;
        }
    }

    /** A playable class. abilityIds are granted (some gated by level). */
    public static final class ClassDef {
        public final ClassId id;
        public final String name, description;
        public final ResourceType resource;
        public final List<String> abilityIds;   // ordered; engine gates by AbilityDef.levelReq
        public ClassDef(ClassId id, String name, ResourceType resource, List<String> abilityIds, String description) {
            this.id = id; this.name = name; this.resource = resource;
            this.abilityIds = Collections.unmodifiableList(new ArrayList<String>(abilityIds));
            this.description = description;
        }
    }

    /** A castable/usable ability. */
    public static final class AbilityDef {
        public final String id, name, description;
        public final ClassId owner;
        public final AbilityKind kind;
        public final TargetType target;
        public final DamageType damageType;
        public final int levelReq;
        public final int resourceCost;      // in the class's ResourceType
        public final double cooldownSec;
        public final double castSec;         // 0 = instant
        public final int power;              // damage/heal magnitude or buff points
        public final double durationSec;     // for DOT/HOT/BUFF/DEBUFF/TRAP; 0 = instant
        public final School school;          // FLAME/FROST/STORM for Magus, else NONE
        public AbilityDef(String id, String name, ClassId owner, AbilityKind kind, TargetType target,
                          DamageType damageType, int levelReq, int resourceCost, double cooldownSec,
                          double castSec, int power, double durationSec, School school, String description) {
            this.id = id; this.name = name; this.owner = owner; this.kind = kind; this.target = target;
            this.damageType = damageType; this.levelReq = levelReq; this.resourceCost = resourceCost;
            this.cooldownSec = cooldownSec; this.castSec = castSec; this.power = power;
            this.durationSec = durationSec; this.school = school; this.description = description;
        }
    }

    public static final class ItemDef {
        public final String id, name, description;
        public final ItemType type;
        public final EquipSlot slot;                 // null unless equippable
        public final Map<StatType, Integer> statMods;
        public final int power;                      // weapon dmg / armor value / consumable magnitude
        public final int value;                      // vendor base price
        public final int stackMax;
        public ItemDef(String id, String name, ItemType type, EquipSlot slot, Map<StatType, Integer> statMods,
                       int power, int value, int stackMax, String description) {
            this.id = id; this.name = name; this.type = type; this.slot = slot;
            this.statMods = Collections.unmodifiableMap(new EnumMap<StatType, Integer>(
                    statMods == null ? new EnumMap<StatType, Integer>(StatType.class) : statMods));
            this.power = power; this.value = value; this.stackMax = Math.max(1, stackMax);
            this.description = description;
        }
    }

    public static final class MobDef {
        public final String id, name;
        public final int level, maxHp, attackPower, defense, xpReward;
        public final MobBehavior behavior;
        public final DamageType damageType;
        public final String spriteKey;               // procedural render key
        public final List<LootDrop> loot;
        public MobDef(String id, String name, int level, int maxHp, int attackPower, int defense,
                      int xpReward, MobBehavior behavior, DamageType damageType, String spriteKey, List<LootDrop> loot) {
            this.id = id; this.name = name; this.level = level; this.maxHp = maxHp;
            this.attackPower = attackPower; this.defense = defense; this.xpReward = xpReward;
            this.behavior = behavior; this.damageType = damageType; this.spriteKey = spriteKey;
            this.loot = Collections.unmodifiableList(new ArrayList<LootDrop>(loot));
        }
    }

    public static final class LootDrop {
        public final String itemId; public final double chance; public final int min, max;
        public LootDrop(String itemId, double chance, int min, int max) {
            this.itemId = itemId; this.chance = chance; this.min = min; this.max = max;
        }
    }

    /** A single quest objective. */
    public static final class ObjectiveDef {
        public final ObjectiveType type;
        public final String targetId;   // mobId / itemId / npcId / zoneId
        public final int count;
        public final String text;
        public ObjectiveDef(ObjectiveType type, String targetId, int count, String text) {
            this.type = type; this.targetId = targetId; this.count = count; this.text = text;
        }
    }

    public static final class QuestDef {
        public final String id, name, description, giverNpcId, turnInNpcId;
        public final int levelReq, xpReward, goldReward;
        public final List<ObjectiveDef> objectives;
        public final List<String> rewardItemIds;
        public final boolean procedural;   // true if produced by the dynamic generator
        public QuestDef(String id, String name, String giverNpcId, String turnInNpcId, int levelReq,
                        int xpReward, int goldReward, List<ObjectiveDef> objectives, List<String> rewardItemIds,
                        boolean procedural, String description) {
            this.id = id; this.name = name; this.giverNpcId = giverNpcId; this.turnInNpcId = turnInNpcId;
            this.levelReq = levelReq; this.xpReward = xpReward; this.goldReward = goldReward;
            this.objectives = Collections.unmodifiableList(new ArrayList<ObjectiveDef>(objectives));
            this.rewardItemIds = Collections.unmodifiableList(new ArrayList<String>(rewardItemIds));
            this.procedural = procedural; this.description = description;
        }
    }

    /** Gather → process → craft chain. inputs/outputs are itemId→qty. */
    public static final class RecipeDef {
        public final String id, name;
        public final SkillType skill;                 // PROCESSING or CRAFTING
        public final int skillReq;
        public final Map<String, Integer> inputs;
        public final String outputItemId;
        public final int outputQty;
        public final double craftSec;
        public RecipeDef(String id, String name, SkillType skill, int skillReq, Map<String, Integer> inputs,
                         String outputItemId, int outputQty, double craftSec) {
            this.id = id; this.name = name; this.skill = skill; this.skillReq = skillReq;
            this.inputs = Collections.unmodifiableMap(new java.util.LinkedHashMap<String, Integer>(inputs));
            this.outputItemId = outputItemId; this.outputQty = outputQty; this.craftSec = craftSec;
        }
    }

    /** A harvestable resource node placed in a zone. */
    public static final class GatherNodeDef {
        public final String id, name, itemId;
        public final SkillType skill;                 // GATHERING
        public final int skillReq, yieldMin, yieldMax;
        public final double respawnSec;
        public GatherNodeDef(String id, String name, String itemId, int skillReq, int yieldMin, int yieldMax, double respawnSec) {
            this.id = id; this.name = name; this.itemId = itemId; this.skill = SkillType.GATHERING;
            this.skillReq = skillReq; this.yieldMin = yieldMin; this.yieldMax = yieldMax; this.respawnSec = respawnSec;
        }
    }

    /** Metadata describing a loadable zone/map (grid dimensions, faction, dungeon flag). */
    public static final class ZoneMeta {
        public final String id, name, description;
        public final int width, height;
        public final Faction faction;      // home faction, or null for contested/frontier
        public final boolean dungeon;      // true = instanced area
        public final int recommendedLevel;
        public ZoneMeta(String id, String name, int width, int height, Faction faction,
                        boolean dungeon, int recommendedLevel, String description) {
            this.id = id; this.name = name; this.width = width; this.height = height;
            this.faction = faction; this.dungeon = dungeon; this.recommendedLevel = recommendedLevel;
            this.description = description;
        }
    }
}
