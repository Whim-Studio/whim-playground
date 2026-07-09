package com.whim.alganon.api;

/**
 * All shared enumerations for the Alganon single-player recreation. Every layer
 * (model / engine / ui) imports these; no layer defines its own copies.
 *
 * <p>Clean-room note: class/race/family/school names mirror Alganon terminology
 * for authenticity in a personal single-player project. No Alganon flavor text or
 * marketing copy is reproduced — all descriptive strings are authored fresh in
 * the content layer.</p>
 */
public final class Enums {
    private Enums() {}

    /** Top-level UI/engine state machine. */
    public enum GameStateType {
        TITLE, CHARACTER_CREATION, PLAYING,
        INVENTORY, CHARACTER_SHEET, QUEST_LOG, STUDY, CRAFTING, AUCTION, FAMILY, LIBRARY,
        SETTINGS, GAME_OVER
    }

    /** The two playable factions. */
    public enum Faction { ASHARR, KUJIX }

    /** Playstyle archetype shared by both factions' family sets. */
    public enum FamilyArchetype { ACHIEVER, COMPETITOR, EXPLORER, SOCIALIZER, CRAFTER }

    /** The six classes. */
    public enum ClassId { CHAMPION, REAVER, RANGER, MAGUS, MYSTIC, CABALIST }

    /** Per-class primary resource. [Gap] resource names are my design (see DESIGN.md). */
    public enum ResourceType { FURY, FOCUS, MANA }

    /** Champion stances. [Anchor] balance/power/defense. */
    public enum Stance { BALANCE, POWER, DEFENSE }

    /** Magus elemental schools. [Anchor] Flame/Frost/Storm. Also used as a soft-spec tag. */
    public enum School { NONE, FLAME, FROST, STORM }

    /** Core character stats. */
    public enum StatType { MIGHT, FINESSE, INTELLECT, SPIRIT, STAMINA }

    /** Skill lines that gain from use and can be assigned to offline Study. */
    public enum SkillType {
        WEAPON, DEFENSE, CASTING, HEALING,          // combat lines
        GATHERING, PROCESSING, CRAFTING             // tradeskill lines
    }

    public enum DamageType { PHYSICAL, FLAME, FROST, STORM, SHADOW, HOLY }

    /** How an ability selects its target(s). */
    public enum TargetType { SELF, ENEMY, ALLY, GROUND, NONE }

    /** What an ability does when it resolves. */
    public enum AbilityKind { DAMAGE, HEAL, BUFF, DEBUFF, DOT, HOT, PET_SUMMON, TRAP, STANCE, UTILITY }

    public enum EquipSlot { HEAD, CHEST, HANDS, LEGS, FEET, WEAPON, OFFHAND, TRINKET }

    public enum ItemType { WEAPON, ARMOR, TRINKET, CONSUMABLE, MATERIAL, QUEST, RECIPE }

    /** Tile semantics for the top-down zone renderer/collision. */
    public enum TileType { GRASS, DIRT, STONE, WATER, WALL, ROAD, SAND, FLOOR, VOID }

    /** Coarse mob behaviour used by the engine's simple AI. */
    public enum MobBehavior { PASSIVE, DEFENSIVE, AGGRESSIVE }

    /** Quest lifecycle. */
    public enum QuestStatus { AVAILABLE, ACTIVE, READY_TO_TURN_IN, COMPLETED }

    /** Quest objective kinds (static + procedurally generated). */
    public enum ObjectiveType { KILL, GATHER, TALK, TRAVEL }

    /** Chat/log channels. Multiplayer tiers are single-player NPC-driven substitutions. */
    public enum ChatChannel { SYSTEM, SAY, FAMILY, FACTION, COMBAT, LOOT }

    /** Cardinal movement in the top-down world. */
    public enum Direction {
        NORTH(0, -1), SOUTH(0, 1), WEST(-1, 0), EAST(1, 0);
        public final int dx, dy;
        Direction(int dx, int dy) { this.dx = dx; this.dy = dy; }
    }

    /** Who currently controls a contested faction-war objective (Towers/Keeps sim). */
    public enum ControlState { ASHARR, KUJIX, CONTESTED, NEUTRAL }
}
