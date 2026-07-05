package com.whim.oggalaxy.api;

/**
 * All stable identifiers (enums) shared across every layer of the game.
 *
 * This file is part of the orchestrator-owned {@code api} contract package and is
 * the single source of truth for the set of resources, buildings, technologies,
 * ships, defenses, missions, player classes and difficulty levels. The simulation
 * task and the UI task both program against these enums and must never invent
 * their own parallel copies.
 *
 * Java 8 only. No language features newer than Java 8 are used anywhere in {@code api}.
 */
public final class Ids {

    private Ids() {
    }

    /** The four classic OGame-style economy resources plus the premium Dark Matter currency. */
    public enum ResourceType {
        METAL,
        CRYSTAL,
        DEUTERIUM,
        ENERGY,      // not stored; balance of production vs consumption drives a factory efficiency factor
        DARK_MATTER  // premium currency, mainly earned from expeditions
    }

    /** Planet (and moon) buildings. Order roughly matches build-menu grouping. */
    public enum BuildingType {
        // --- resource buildings ---
        METAL_MINE,
        CRYSTAL_MINE,
        DEUTERIUM_SYNTHESIZER,
        SOLAR_PLANT,
        FUSION_REACTOR,
        METAL_STORAGE,
        CRYSTAL_STORAGE,
        DEUTERIUM_TANK,
        // --- facility buildings ---
        ROBOTICS_FACTORY,
        SHIPYARD,
        RESEARCH_LAB,
        NANITE_FACTORY,
        TERRAFORMER,
        MISSILE_SILO,
        // --- moon buildings ---
        LUNAR_BASE,
        SENSOR_PHALANX,
        JUMP_GATE
    }

    /** Research technologies. */
    public enum TechType {
        ENERGY_TECHNOLOGY,
        LASER_TECHNOLOGY,
        ION_TECHNOLOGY,
        HYPERSPACE_TECHNOLOGY,
        PLASMA_TECHNOLOGY,
        COMBUSTION_DRIVE,
        IMPULSE_DRIVE,
        HYPERSPACE_DRIVE,
        ESPIONAGE_TECHNOLOGY,
        COMPUTER_TECHNOLOGY,
        ASTROPHYSICS,
        GRAVITON_TECHNOLOGY,
        WEAPONS_TECHNOLOGY,
        SHIELDING_TECHNOLOGY,
        ARMOUR_TECHNOLOGY
    }

    /** Ship classes. Cargo/combat/civil roles are distinguished by {@link ShipDef}. */
    public enum ShipType {
        SMALL_CARGO,
        LARGE_CARGO,
        LIGHT_FIGHTER,
        HEAVY_FIGHTER,
        CRUISER,
        BATTLESHIP,
        BATTLECRUISER,
        BOMBER,
        DESTROYER,
        REAPER,
        PATHFINDER,
        LEVIATHAN,      // OG Galaxy super-capital flagship (approximated stats — see Catalog)
        DEATHSTAR,
        RECYCLER,
        ESPIONAGE_PROBE,
        SOLAR_SATELLITE,
        COLONY_SHIP
    }

    /** Planetary defenses (built in the shipyard, never leave the planet). */
    public enum DefenseType {
        ROCKET_LAUNCHER,
        LIGHT_LASER,
        HEAVY_LASER,
        ION_CANNON,
        GAUSS_CANNON,
        PLASMA_TURRET,
        SMALL_SHIELD_DOME,
        LARGE_SHIELD_DOME
    }

    /** Fleet mission types selectable in the dispatch dialog. */
    public enum MissionType {
        ATTACK,
        TRANSPORT,
        DEPLOY,       // move ships/resources to an owned planet
        COLONIZE,
        EXPEDITION,
        RECYCLE,      // harvest a debris field
        ESPIONAGE     // send probes to scout
    }

    /** Player / empire specialization. Bonuses are defined in {@link ClassDef}. */
    public enum PlayerClass {
        EXPLORER,   // expeditions & movement
        MINER,      // economy & production
        GENERAL     // military & combat
    }

    /** AI difficulty. RANDOM is resolved to one concrete level at game creation. */
    public enum Difficulty {
        EASY,
        MEDIUM,
        HARD,
        RANDOM
    }

    /** High-level game phase / status used by the controller and UI. */
    public enum Phase {
        SETUP,
        RUNNING,
        PAUSED,
        VICTORY,
        DEFEAT
    }

    /** Category tag for a log / event-feed entry, used to colour the UI log panel. */
    public enum LogCategory {
        ECONOMY,
        RESEARCH,
        CONSTRUCTION,
        FLEET,
        COMBAT,
        EXPEDITION,
        ESPIONAGE,
        AI,
        SYSTEM
    }
}
