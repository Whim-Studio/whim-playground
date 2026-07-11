package com.whim.bc3k.api;

import java.util.List;

/**
 * Read-only projections the UI renders from. The engine implements these; console
 * screens ONLY read them and never cast to a concrete model class.
 *
 * Phase 4 extends the seam with the galaxy (star map), the crew roster, and combat
 * state so Free Flight and Xtreme Carnage can be rendered and driven.
 */
public final class Views {
    private Views() {}

    /** Root snapshot. Everything the UI needs hangs off here. */
    public interface GameView {
        Enums.Mode mode();
        Enums.GameMode gameMode();
        boolean paused();
        boolean started();
        ShipView ship();
        GalaxyView galaxy();
        List<CrewView> crew();
        CargoView cargo();
        List<CraftView> craft();
        CombatView combat();        // null unless a combat is active
        CampaignView campaign();    // null unless in Advanced Campaign mode
        List<String> log();
        String flash();             // transient mission banner, empty when idle
    }

    /** Advanced Campaign Mode state (COMMS console). */
    public interface CampaignView {
        int threat();               // 0..100 Gammulan pressure
        boolean critical();
        int resolved();             // objectives completed
        String objective();         // current GALCOM objective
    }

    /** Logistics: credits, fuel and consumables (CARGO console). */
    public interface CargoView {
        int credits();
        int fuel();
        int maxFuel();
        int spareParts();
        int ordnance();
    }

    /** A carried small-craft complement (FLIGHT DECK console). */
    public interface CraftView {
        Enums.CraftType type();
        int total();
        int launched();
        int docked();               // total - launched
    }

    /** The player's battlecruiser. */
    public interface ShipView {
        String name();
        int hull();
        int maxHull();
        int shields();
        int maxShields();
        boolean reactorOnline();
        int reactorOutput();
        int reactorUsed();
        int power(Enums.PowerSystem s);
        int integrity(Enums.PowerSystem s);   // 0..100 subsystem health
        boolean breached(Enums.PowerSystem s);
        int maxPerSystem();
        Enums.Alert alert();
    }

    /** One crew member for the PERSONNEL console. */
    public interface CrewView {
        int id();
        String name();
        int health();
        int fatigue();
        int hunger();
        String location();
        boolean alive();
    }

    /** The navigation star map. */
    public interface GalaxyView {
        int currentId();
        List<SystemView> systems();
    }

    public interface SystemView {
        int id();
        String name();
        int x();
        int y();
        boolean visited();
        boolean hasStation();
        boolean current();
        boolean reachable();       // directly jump-linked from the current system
        List<Integer> links();
    }

    /** Xtreme Carnage combat snapshot. */
    public interface CombatView {
        String enemyName();
        int enemyHull();
        int enemyMaxHull();
        int enemyShields();
        int enemyMaxShields();
        boolean over();
        boolean playerWon();
    }
}
