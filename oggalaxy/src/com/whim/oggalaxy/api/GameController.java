package com.whim.oggalaxy.api;

import java.io.File;

/**
 * THE single seam between the UI and the simulation. The UI holds a
 * {@code GameController}; it never touches simulation classes directly. The
 * simulation task provides the real implementation ({@code GameEngine}); the UI task
 * develops against a {@code StubController} that returns canned snapshots so it can run
 * before the engine lands.
 *
 * Threading contract: {@link #state()} and {@link #catalog()} are safe to call from the
 * EDT at any time. Command methods are safe to call from the EDT; the engine applies
 * them on its own thread and the change shows up in the next {@link #state()} snapshot.
 * The engine must never block the EDT.
 */
public interface GameController {

    /** Static game database (never changes during a game). */
    Catalog catalog();

    /** Latest immutable-enough snapshot of the whole visible world. Never null after newGame. */
    Views.GameStateView state();

    // ---- lifecycle ----
    void newGame(NewGameSetup setup);
    void startClock();          // begin auto-advancing time on the background thread
    void stopClock();           // pause auto-advance
    boolean isClockRunning();

    /** Set auto-advance speed: number of simulation ticks applied per real second. */
    void setSpeed(int ticksPerSecond);
    int getSpeed();

    /** Manually advance the simulation by {@code ticks} ticks (used by the "Advance Turn" button). */
    void advance(int ticks);

    // ---- player commands (all validated; return a Result the UI can surface) ----
    Result enqueueBuilding(String planetId, Ids.BuildingType type);
    Result cancelConstruction(String planetId);
    Result enqueueResearch(Ids.TechType type, String labPlanetId);
    Result enqueueShip(String planetId, Ids.ShipType type, int count);
    Result enqueueDefense(String planetId, Ids.DefenseType type, int count);
    Result dispatchFleet(FleetOrder order);
    Result recallFleet(String fleetId);
    void selectPlanet(String planetId);

    // ---- persistence ----
    Result save(File file);
    Result load(File file);

    // ---- optional push notifications (UI may also just poll state()) ----
    void addListener(GameListener listener);
    void removeListener(GameListener listener);

    /** Push callback; always delivered on the EDT by the engine. */
    interface GameListener {
        void onTick(Views.GameStateView state);
        void onEvent(Views.LogEntryView event);
    }
}
