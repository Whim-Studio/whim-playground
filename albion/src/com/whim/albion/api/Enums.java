package com.whim.albion.api;

/**
 * All shared enumerations for the Albion recreation. These names describe game
 * <em>mechanics</em> only (a clean-room re-implementation); no copyrighted text,
 * lore, or asset names from the original 1995 game are reproduced. Magic-school
 * names are intentionally generic-descriptive rather than the original's names.
 */
public final class Enums {

    private Enums() {}

    /** High-level application / game states driven by the state machine. */
    public enum GameStateType {
        TITLE, OVERWORLD, DUNGEON, COMBAT, DIALOGUE, INVENTORY,
        CHARACTER_SHEET, JOURNAL, MENU, GAME_OVER
    }

    /** How a map is presented and navigated. */
    public enum MapType {
        /** Bird's-eye tile map (towns, overworld). */
        OUTDOOR_2D,
        /** Grid-based first-person "blobber" (dungeons, interiors). */
        INDOOR_3D
    }

    /** Semantic tile category — a rendering + collision hint. */
    public enum TileType {
        GRASS, PATH, FLOOR, WALL, WATER, DOOR, OBSTACLE, VOID, STAIRS
    }

    /** Cardinal facing / movement direction. Screen y grows downward. */
    public enum Direction {
        NORTH(0, -1), EAST(1, 0), SOUTH(0, 1), WEST(-1, 0);

        private final int dx;
        private final int dy;

        Direction(int dx, int dy) { this.dx = dx; this.dy = dy; }

        public int dx() { return dx; }
        public int dy() { return dy; }

        public Direction left() {
            switch (this) {
                case NORTH: return WEST;
                case WEST:  return SOUTH;
                case SOUTH: return EAST;
                default:    return NORTH;
            }
        }

        public Direction right() {
            switch (this) {
                case NORTH: return EAST;
                case EAST:  return SOUTH;
                case SOUTH: return WEST;
                default:    return NORTH;
            }
        }

        public Direction opposite() {
            switch (this) {
                case NORTH: return SOUTH;
                case SOUTH: return NORTH;
                case EAST:  return WEST;
                default:    return EAST;
            }
        }
    }

    /** Primary character attributes. */
    public enum StatType {
        STRENGTH, INTELLIGENCE, DEXTERITY, SPEED, STAMINA, LUCK,
        MAGIC_TALENT, MAGIC_RESISTANCE
    }

    /** Trainable percentage skills. */
    public enum SkillType {
        MELEE, RANGED, CRITICAL, LOCKPICKING
    }

    /** Damage / resistance channels. */
    public enum DamageType {
        PHYSICAL, FIRE, COLD, MAGIC, POISON
    }

    /**
     * Magic disciplines. Names are generic-descriptive (clean-room); a character's
     * profession/race gates which schools they may learn from.
     */
    public enum SpellSchool {
        PSIONIC, DESTRUCTION, NATURE, RESTORATION
    }

    /** Equipment slots on a character. */
    public enum EquipSlot {
        WEAPON, SHIELD, HEAD, BODY, ACCESSORY
    }

    /** Item categories. */
    public enum ItemType {
        WEAPON, ARMOR, SHIELD, HELMET, CONSUMABLE, SCROLL, KEY, QUEST, AMMO, MISC
    }

    /** Who/what a spell or action can be aimed at. */
    public enum TargetType {
        SELF, SINGLE_ALLY, SINGLE_ENEMY, ALL_ALLIES, ALL_ENEMIES
    }

    /** Actions selectable on a combatant's turn. */
    public enum CombatActionType {
        ATTACK, CAST, ITEM, MOVE, DEFEND, FLEE
    }

    /** Enemy tactical archetypes for combat AI. */
    public enum EnemyBehaviorType {
        AGGRESSIVE, RANGED, SUPPORT
    }

    /** What a spell does when resolved. */
    public enum SpellEffectType {
        DAMAGE, HEAL, BUFF, DEBUFF, UTILITY
    }

    /** Lifecycle of a journal quest entry. */
    public enum QuestStatus {
        ACTIVE, COMPLETED, FAILED
    }
}
