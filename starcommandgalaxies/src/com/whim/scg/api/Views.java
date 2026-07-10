package com.whim.scg.api;

import java.util.List;

/**
 * Read-only projections the UI renders from. The engine (Task 1) implements
 * these; the UI tasks (Task 2 / Task 3) ONLY read them and never cast to a
 * concrete model class. Keeping the UI on these interfaces is what lets the
 * three tasks build in parallel.
 */
public final class Views {
    private Views() {}

    /** Root snapshot. Everything the UI needs hangs off here. */
    public interface GameView {
        Enums.Mode mode();
        int credits();
        int day();
        ShipView playerShip();
        GalaxyView galaxy();
        CombatView combat();       // null unless in SPACE_COMBAT
        BoardingView boarding();   // null unless in BOARDING
        List<TechView> techTree();
        List<String> log();        // recent event/battle log lines (newest last)
        boolean paused();
        String flash();            // transient banner text, may be empty
    }

    public interface ShipView {
        String name();
        Enums.Faction faction();
        int hull();
        int maxHull();
        int shields();
        int maxShields();
        int reactor();             // total power available
        int reactorUsed();
        int oxygen();              // 0..100 ship-wide
        int gridW();
        int gridH();
        List<RoomView> rooms();
        List<CrewView> crew();
        List<WeaponView> weapons();
        RoomView roomAt(GridPos p);
    }

    public interface RoomView {
        int id();
        Enums.RoomType type();
        GridPos origin();          // top-left cell
        int w();
        int h();
        int power();               // power allocated
        int maxPower();
        int hp();                  // system integrity 0..maxHp
        int maxHp();
        boolean onFire();
        boolean breached();
        List<Integer> crewIds();   // crew currently stationed here
        boolean contains(GridPos p);
    }

    public interface CrewView {
        int id();
        String name();
        Enums.Faction faction();
        Enums.CrewRole role();
        int hp();
        int maxHp();
        int happiness();           // 0..100
        int level();
        int xp();
        int skill(Enums.StatType s);
        int stationRoomId();       // -1 if unassigned
        GridPos boardingPos();     // position during boarding, else null
        boolean alive();
    }

    public interface WeaponView {
        int slot();
        String name();
        Enums.WeaponType type();
        int damage();
        int chargeMax();           // ticks to charge
        int charge();              // current charge
        boolean ready();
        int powered();             // power allocated (0 = off)
        int targetRoomId();        // -1 if no target set
    }

    public interface GalaxyView {
        int width();
        int height();
        int currentSystem();       // id of system the player is at
        List<StarSystemView> systems();
    }

    public interface StarSystemView {
        int id();
        String name();
        int x();
        int y();
        boolean visited();
        boolean hasStarport();
        Enums.EventType pendingEvent();
        List<Integer> links();     // ids of directly reachable systems
    }

    /** Space combat snapshot (continuous space). */
    public interface CombatView {
        ShipView player();
        ShipView enemy();
        List<ProjectileView> projectiles();
        boolean canBoard();        // enemy shields down + in teleporter range
        boolean over();
        boolean playerWon();
    }

    public interface ProjectileView {
        Vec2 pos();
        Enums.WeaponType type();
        boolean fromPlayer();
    }

    /** Boarding / away-mission snapshot (tile grid). */
    public interface BoardingView {
        int gridW();
        int gridH();
        Enums.TileType tileAt(GridPos p);
        List<CrewView> friendlies();
        List<CrewView> hostiles();
        int selectedCrewId();      // -1 if none
        boolean over();
        boolean playerWon();
        String objective();
    }

    public interface TechView {
        Enums.TechType type();
        int level();
        int maxLevel();
        int cost();                // credits to buy next level
        boolean maxed();
    }
}
