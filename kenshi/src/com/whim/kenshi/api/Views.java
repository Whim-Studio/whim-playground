package com.whim.kenshi.api;

import java.util.List;

/**
 * Read-only, per-frame snapshot interfaces. The engine mutates concrete domain
 * objects on its tick thread; the UI polls {@link GameController#state()} and
 * reads ONLY these interfaces. The UI must never cast a *View down to a concrete
 * domain or engine class. Snapshots are treated as immutable for the frame.
 */
public final class Views {

    private Views() {}

    /** A character's snapshot for one frame. */
    public interface CharacterView {
        String id();
        String name();
        Enums.FactionId faction();

        double x();
        double y();
        /** Facing angle in radians (0 = +x, counter-clockwise). */
        double heading();

        Enums.MoveState moveState();
        Enums.AiState aiState();
        boolean selected();
        boolean playerControlled();
        boolean alive();
        boolean downed();

        // --- anatomy ---
        /** Current HP of a part (may be negative down to -max). */
        double partHp(Enums.BodyPart part);
        /** Maximum HP of a part. */
        double partMax(Enums.BodyPart part);
        /** True if the part is disabled (hp <= 0). */
        boolean partDisabled(Enums.BodyPart part);

        double hunger();       // 0..HUNGER_MAX
        double hungerMax();
        double blood();        // 0..BLOOD_MAX
        double bloodMax();
        /** Current bleed rate (HP-equivalent lost per world second); 0 if stable. */
        double bleedRate();

        Enums.WeaponClass weapon();
        int skill(Enums.SkillType skill);

        // --- current order / combat ---
        Enums.OrderType orderType();
        /** Target id for ATTACK orders / current combat target, or null. */
        String targetId();
    }

    /** A player or AI squad grouping. */
    public interface SquadView {
        String id();
        String name();
        Enums.FactionId faction();
        List<String> memberIds();
    }

    /** Faction disposition, exposed for the HUD's reputation readout. */
    public interface FactionView {
        Enums.FactionId id();
        String label();
        Enums.Relation relationTo(Enums.FactionId other);
        /** Player-facing reputation score with this faction (-100..100). */
        int reputationWithPlayer();
    }

    /** An interactable location on the map. */
    public interface NodeView {
        String id();
        String name();
        Enums.NodeType type();
        Enums.FactionId owner();
        double x();
        double y();
        double radius();
    }

    /** The static-ish world map. Terrain may be read every frame but rarely
     * changes. Tile coordinates are (col, row) in [0, Config.MAP_TILES). */
    public interface MapView {
        int tiles();                       // == Config.MAP_TILES
        double tileSize();                 // == Config.TILE_SIZE
        Enums.Terrain terrain(int col, int row);
    }

    /** A single line for the on-screen event log (most recent last). */
    public interface LogView {
        long tick();
        String text();
    }

    /** The whole observable world for one frame. */
    public interface GameStateView {
        Enums.Phase phase();
        long tick();
        /** Elapsed in-world seconds. */
        double worldSeconds();
        int gameSpeed();                   // 1,2,4 ... (0 while paused)

        MapView map();
        List<CharacterView> characters();
        List<SquadView> squads();
        List<FactionView> factions();
        List<NodeView> nodes();

        /** Ids currently selected by the player. */
        List<String> selectedIds();
        List<LogView> log();
    }
}
