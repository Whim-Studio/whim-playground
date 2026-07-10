package com.whim.scg.api;

/**
 * All shared enumerations for the Star Command Galaxies recreation.
 * Every task compiles against these; do NOT add task-specific values without
 * updating the CONTRACT and notifying the orchestrator.
 */
public final class Enums {
    private Enums() {}

    /** Top-level UI / game mode. Drives which Screen the shell shows. */
    public enum Mode {
        MENU, SHIP_INTERIOR, GALAXY_MAP, STARPORT, SPACE_COMBAT, BOARDING, GAME_OVER, VICTORY
    }

    /**
     * Functional room types. A room may host at most one powered system.
     * POWERED helper marks rooms that consume reactor power and can be manned.
     */
    public enum RoomType {
        BRIDGE(true), ENGINES(true), WEAPONS(true), SHIELDS(true), MEDBAY(true),
        TELEPORTER(true), OXYGEN(true), SENSORS(true), QUARTERS(false), CARGO(false),
        CORRIDOR(false);

        private final boolean powered;
        RoomType(boolean powered) { this.powered = powered; }
        public boolean isPowered() { return powered; }
    }

    /** Crew job specialities. Each maps to one primary StatType. */
    public enum CrewRole {
        CAPTAIN(StatType.COMMAND), PILOT(StatType.PILOTING), ENGINEER(StatType.ENGINEERING),
        GUNNER(StatType.GUNNERY), SHIELD_TECH(StatType.SHIELDS), MEDIC(StatType.MEDICAL),
        SECURITY(StatType.COMBAT), SCIENCE(StatType.SCIENCE);

        private final StatType primary;
        CrewRole(StatType primary) { this.primary = primary; }
        public StatType primary() { return primary; }
    }

    /** Crew skill axes (0..100). Leveling raises these. */
    public enum StatType {
        COMMAND, PILOTING, ENGINEERING, GUNNERY, SHIELDS, MEDICAL, COMBAT, SCIENCE
    }

    /** Weapon families for space combat. */
    public enum WeaponType {
        LASER, ION, PLASMA_TORPEDO, MISSILE, BEAM
    }

    /** Damage flavours applied to rooms/hull/crew. */
    public enum DamageType { HULL, ION, FIRE, BREACH, KINETIC }

    /** Factions that own ships and crew. FEDERATION is the player. */
    public enum Faction { FEDERATION, PIRATE, ALIEN_SWARM, MERCHANT, NEUTRAL }

    /** Random galaxy encounter categories. */
    public enum EventType { NOTHING, DERELICT, DISTRESS, MERCHANT, HAZARD, PIRATE_AMBUSH, STORY }

    /** Boarding / away-mission tile types. */
    public enum TileType { FLOOR, WALL, DOOR, SYSTEM, HAZARD }

    /** 4-way movement used on grids (boarding + interior). */
    public enum Direction {
        NORTH(0, -1), SOUTH(0, 1), EAST(1, 0), WEST(-1, 0);
        public final int dx, dy;
        Direction(int dx, int dy) { this.dx = dx; this.dy = dy; }
    }

    /** Upgrade / research tracks in the tech tree. */
    public enum TechType { WEAPONS, SHIELDS, ENGINES, HULL, MEDBAY, TELEPORTER, SENSORS }
}
