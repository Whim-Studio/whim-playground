package com.whim.kenshi.api;

/**
 * Shared enumerations for the whole game. Kept in one file so all three tasks
 * agree on the exact set of values. Owned by the orchestrator — do not add or
 * rename values in a task package.
 */
public final class Enums {

    private Enums() {}

    /** The seven independent health pools every character owns. Order is stable
     * and is the canonical iteration order used by the HUD. */
    public enum BodyPart {
        HEAD("Head", true),
        CHEST("Chest", true),
        STOMACH("Stomach", true),
        LEFT_ARM("Left Arm", false),
        RIGHT_ARM("Right Arm", false),
        LEFT_LEG("Left Leg", false),
        RIGHT_LEG("Right Leg", false);

        private final String label;
        private final boolean vital; // dropping a vital part below its floor -> death

        BodyPart(String label, boolean vital) {
            this.label = label;
            this.vital = vital;
        }

        public String label() { return label; }
        public boolean vital() { return vital; }
        public boolean isArm() { return this == LEFT_ARM || this == RIGHT_ARM; }
        public boolean isLeg() { return this == LEFT_LEG || this == RIGHT_LEG; }
    }

    /** Trainable skills. Gain XP by performing the matching action or by taking
     * damage (Toughness). */
    public enum SkillType {
        TOUGHNESS, ATHLETICS, STRENGTH, DEXTERITY, MELEE_ATTACK, MELEE_DEFENCE
    }

    /** Coarse physical state, derived by the engine from anatomy each tick. */
    public enum MoveState {
        IDLE,      // standing, able-bodied
        MOVING,    // walking/running to a destination
        CRAWLING,  // both legs disabled -> slow crawl
        DOWNED,    // unconscious (vital part or blood too low) but alive
        DEAD
    }

    /** Weapon class a character is wielding. Two-handed weapons need both arms. */
    public enum WeaponClass {
        UNARMED, ONE_HANDED, TWO_HANDED
    }

    /** The world's factions. PLAYER is the squad the user controls. */
    public enum FactionId {
        PLAYER("Player Squad"),
        HOLY_NATION("Holy Nation"),
        SHEK("Shek Kingdom"),
        DUST_BANDITS("Dust Bandits"),
        HUNGRY_BANDITS("Hungry Bandits"),
        TRADE_GUILD("Trade Guild"),
        DRIFTERS("Drifters");

        private final String label;
        FactionId(String label) { this.label = label; }
        public String label() { return label; }
    }

    /** Pairwise disposition between two factions. */
    public enum Relation { ALLY, NEUTRAL, HOSTILE }

    /** AI behaviour state for non-player characters (and idle player units). */
    public enum AiState {
        IDLE, WANDER, PATROL, PURSUE, ATTACK, FLEE, LOOT, RETURN
    }

    /** What the engine is currently doing overall. */
    public enum Phase { RUNNING, PAUSED, GAME_OVER }

    /** Terrain type of a map tile; drives colour and (optionally) move cost. */
    public enum Terrain {
        SAND, SCRUB, GREEN, ROCK, ASH, WATER, TOWN
    }

    /** Kinds of interactable world node the player can right-click. */
    public enum NodeType { TOWN, BAR, SHOP, CAMP, RUIN }

    /** A queued order attached to a character by the engine. Exposed for the HUD. */
    public enum OrderType { NONE, MOVE, ATTACK, INTERACT }
}
