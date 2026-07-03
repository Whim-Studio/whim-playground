package com.whim.powermonger.api;

/**
 * Shared enumerations for the Powermonger engine. Owned by the orchestrator (api).
 * Do not modify in Task 1/2/3 — import and use only.
 */
public final class Enums {
    private Enums() {}

    /**
     * Command posture. The number of swords (1/2/3) shown in the UI maps directly
     * to the fraction of the maximum action scale the engine applies:
     * Passive = 25%, Neutral = 50%, Aggressive = 100%.
     */
    public enum Posture {
        PASSIVE(1, 0.25),
        NEUTRAL(2, 0.50),
        AGGRESSIVE(3, 1.00);

        private final int swords;
        private final double scale;

        Posture(int swords, double scale) {
            this.swords = swords;
            this.scale = scale;
        }

        /** 1, 2, or 3 — number of sword icons the console draws. */
        public int swords() { return swords; }

        /** 0.25, 0.50, or 1.00 — fraction of full effect the engine applies. */
        public double scale() { return scale; }

        public Posture cycleUp() {
            switch (this) {
                case PASSIVE: return NEUTRAL;
                case NEUTRAL: return AGGRESSIVE;
                default: return AGGRESSIVE;
            }
        }

        public Posture cycleDown() {
            switch (this) {
                case AGGRESSIVE: return NEUTRAL;
                case NEUTRAL: return PASSIVE;
                default: return PASSIVE;
            }
        }
    }

    /** Indirect orders issued to a Captain's army bloc via the command console. */
    public enum CommandType {
        SCOUT("Scout"),
        FIGHT("Fight"),
        GATHER_FOOD("Gather Food"),
        SUPPLY_FOOD("Supply Food"),
        RECRUIT("Recruit"),
        DISBAND("Disband"),
        INVENT("Invent"),
        TRADE("Trade"),
        MOVE("Move");

        private final String label;
        CommandType(String label) { this.label = label; }
        public String label() { return label; }
    }

    /** Terrain classes for the topographical map. */
    public enum TerrainType {
        DEEP_WATER,
        SHALLOW_WATER,
        BEACH,
        GRASS,
        FOREST,
        HILL,
        MOUNTAIN,
        TOWN
    }

    /** Seasonal cycle. Drives weather probabilities and snow cover. */
    public enum Season {
        SPRING, SUMMER, AUTUMN, WINTER
    }

    /** Active weather. Rain/snow slow movement; snow paints the map white. */
    public enum Weather {
        CLEAR, RAIN, SNOW
    }

    /** Faction allegiance of a town, person, or bloc. */
    public enum Allegiance {
        PLAYER, ENEMY, NEUTRAL
    }

    /** Autonomous jobs performed by neutral townspeople ("Artificial Life"). */
    public enum Job {
        FARMING, FISHING, HERDING, CRAFTING, IDLE
    }
}
