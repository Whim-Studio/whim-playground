package com.whim.cardwoven.api;

/**
 * Shared enumerations for Cardwoven Empires. Grouped in one file so the whole
 * vocabulary of the game is visible in a single place. Java 8 only.
 */
public final class Enums {
    private Enums() {}

    /** The three playable factions. */
    public enum Faction {
        LANDS_OF_THE_KING("Lands of the King", 6),  // balanced, high draw
        BABYLON("Babylon", 5),                       // building-focused
        THE_UNFAITHFUL("The Unfaithful", 5);         // power now, sin cards later

        private final String display;
        private final int baseHandSize;

        Faction(String display, int baseHandSize) {
            this.display = display;
            this.baseHandSize = baseHandSize;
        }

        public String display() { return display; }
        /** Cards drawn to hand each turn before faction/building bonuses. */
        public int baseHandSize() { return baseHandSize; }
    }

    /** Buildings placed onto the grid by playing BUILDING cards. */
    public enum BuildingType {
        CITY("City"),
        FARM("Farm"),
        TEMPLE("Temple"),
        PORT("Port");

        private final String display;
        BuildingType(String display) { this.display = display; }
        public String display() { return display; }
    }

    /** Terrain of each map tile. Some buildings require specific terrain. */
    public enum TerrainType {
        PLAINS, FOREST, MOUNTAIN, WATER, DESERT
    }

    /** High-level classification of a card. */
    public enum CardType {
        BUILDING,    // places a BuildingType on the grid
        ATTACHMENT,  // attaches to an existing building for yields
        MILITARY,    // used in combat
        ECONOMY,     // one-shot gold / command point gain
        EXPLORE,     // reveals map tiles
        SIN          // dead card that clogs The Unfaithful deck
    }

    /** Attachment cards attach to buildings and apply buff modifiers. */
    public enum AttachmentType {
        WORKER("Worker"),   // Cities -> Gold
        IDOL("Idol"),       // Temples -> card draw
        WITCH("Witch");     // Temples/Cities -> Command Points

        private final String display;
        AttachmentType(String display) { this.display = display; }
        public String display() { return display; }
    }

    /** Tracked resources. */
    public enum ResourceType {
        GOLD("Gold"),
        COMMAND_POINTS("Command");

        private final String display;
        ResourceType(String display) { this.display = display; }
        public String display() { return display; }
    }

    /** Turn phases, executed in order. */
    public enum GamePhase {
        DRAW, MAIN, COMBAT, YIELD, DISCARD, END
    }

    /** Five distinct victory conditions. Availability varies by faction. */
    public enum VictoryType {
        ECONOMIC("Economic", "Accumulate a gold treasury threshold"),
        MILITARY("Military", "Destroy enough enemy/raider forces"),
        EXPANSION("Expansion", "Control a threshold of map tiles/buildings"),
        FAITH("Faith", "Amass Temples and Idols (or purge all Sin)"),
        DOMINANCE("Dominance", "Reach a total command-point supremacy");

        private final String display;
        private final String summary;
        VictoryType(String display, String summary) {
            this.display = display;
            this.summary = summary;
        }
        public String display() { return display; }
        public String summary() { return summary; }
    }
}
