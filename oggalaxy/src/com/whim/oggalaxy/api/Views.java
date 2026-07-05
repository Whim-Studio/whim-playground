package com.whim.oggalaxy.api;

import java.util.List;
import java.util.Map;

/**
 * Read-only per-poll snapshot interfaces. The UI reads ONLY these (plus the static
 * {@link Catalog}) and never casts to a concrete simulation class. The simulation's
 * model classes implement these interfaces and are handed straight to the UI.
 *
 * Everything here is a live read of immutable-enough state: the UI polls
 * {@link GameController#state()} on a Swing timer and repaints. Implementations must
 * be safe to read from the EDT while the engine ticks on its own thread (the engine
 * publishes fresh snapshots atomically).
 */
public final class Views {

    private Views() {
    }

    /** Resource amounts, caps and per-tick net production for a single planet. */
    public interface ResourceView {
        double amount(Ids.ResourceType type);
        double capacity(Ids.ResourceType type);     // metal/crystal/deut; others return +inf
        double productionPerTick(Ids.ResourceType type); // net (production - consumption)
        double energyProduced();
        double energyConsumed();
        double energyRatio();                        // 0..1 factory efficiency
    }

    /** A single item currently being produced (building, research, ship batch or defense batch). */
    public interface QueueItemView {
        String label();          // e.g. "Metal Mine 8" or "12x Cruiser"
        int totalTicks();
        int remainingTicks();
        double progressFraction();
    }

    /** A planet or moon. */
    public interface PlanetView {
        String id();
        String name();
        int galaxy();
        int system();
        int position();
        boolean isMoon();
        boolean hasMoon();          // for a planet: does it have an attached moon?
        int minTemp();
        int maxTemp();
        int fieldsUsed();
        int fieldsMax();
        int buildingLevel(Ids.BuildingType type);
        int shipCount(Ids.ShipType type);
        int defenseCount(Ids.DefenseType type);
        ResourceView resources();
        QueueItemView currentConstruction();   // building/research on this planet, or null
        List<QueueItemView> shipyardQueue();    // ship/defense batches queued here
    }

    /** A player or AI empire. */
    public interface EmpireView {
        String id();
        String name();
        boolean isAI();
        boolean isPlayer();
        Ids.PlayerClass playerClass();
        Ids.Difficulty difficulty();   // null for the human player
        boolean alive();
        long score();
        int techLevel(Ids.TechType type);
        List<PlanetView> planets();
        QueueItemView currentResearch(); // empire-wide research in progress, or null
    }

    /** A fleet in flight (visible to the player: own fleets and hostile inbound). */
    public interface FleetMovementView {
        String id();
        String ownerName();
        boolean ownedByPlayer();
        Ids.MissionType mission();
        int[] origin();   // {galaxy, system, position}
        int[] target();
        Map<Ids.ShipType, Integer> ships();
        Cost cargo();
        int departTick();
        int arrivalTick();
        int returnTick();
        boolean returning();
        String statusText();
    }

    /** One position in a galaxy/system view. */
    public interface GalaxyCellView {
        int galaxy();
        int system();
        int position();
        boolean empty();
        String ownerName();
        boolean ownedByPlayer();
        boolean isAI();
        String planetName();
        boolean hasMoon();
        boolean hasDebris();
        Cost debris();
    }

    /** An event-feed entry. */
    public interface LogEntryView {
        int tick();
        String timeText();
        Ids.LogCategory category();
        String message();
    }

    /** A resolved deterministic combat report. */
    public interface CombatReportView {
        String id();
        int tick();
        String attackerName();
        String defenderName();
        int[] location();               // {g,s,p}
        List<String> roundSummaries();  // one line per combat round
        Map<Ids.ShipType, Integer> attackerLosses();
        Map<Ids.ShipType, Integer> defenderShipLosses();
        Map<Ids.DefenseType, Integer> defenderDefenseLosses();
        Cost debris();
        Cost plunder();
        boolean moonCreated();
        String outcome();
        String fullText();
    }

    /** An expedition outcome report. */
    public interface ExpeditionReportView {
        String id();
        int tick();
        String outcome();     // e.g. "Resources found", "Pirates!", "Nothing", "Fleet lost"
        String detail();
        Cost gains();
        int darkMatter();
    }

    /** The whole visible game world for one poll. */
    public interface GameStateView {
        int currentTick();
        String formattedTime();
        Ids.Phase phase();
        EmpireView player();
        List<EmpireView> empires();          // includes the player
        List<FleetMovementView> fleets();    // player-visible fleets
        String selectedPlanetId();
        PlanetView selectedPlanet();
        List<LogEntryView> log();            // most-recent-last
        List<GalaxyCellView> galaxyRow(int galaxy, int system);
        List<CombatReportView> combatReports();
        List<ExpeditionReportView> expeditionReports();
        int usedFleetSlots();
        int maxFleetSlots();
        String winnerName();                 // null unless phase is VICTORY/DEFEAT
    }
}
