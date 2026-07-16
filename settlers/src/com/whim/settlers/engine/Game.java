package com.whim.settlers.engine;

import com.whim.settlers.buildings.Building;
import com.whim.settlers.economy.Economy;
import com.whim.settlers.ui.Notifications;

/**
 * Top-level application controller and state machine that sits above {@link
 * World}. It routes the game through MAIN MENU → NEW-GAME SETUP → interactive
 * Castle founding → IN-GAME (with a pause overlay) → VICTORY / DEFEAT, owns the
 * per-run {@link World} and setup config, and derives lightweight event
 * notifications by watching world state each tick.
 *
 * <p>Input and rendering are dispatched by {@link #state()}; the world only
 * advances while {@link State#PLAYING}.
 */
public final class Game {

    public enum State { MENU, SETUP, PLACING_CASTLE, PLAYING, PAUSED, VICTORY, DEFEAT }

    private State state = State.MENU;
    private World world;
    private final SetupConfig config = new SetupConfig();
    private final Notifications notifications = new Notifications();

    private boolean helpVisible;
    private String setupError; // shown on the setup screen (e.g. no Castle site)

    // Notification-tracking snapshots.
    private int lastHumanBuildings;
    private boolean wasUnderAttack;
    private boolean warnedNoTrees;

    // --- accessors ---
    public State state()                 { return state; }
    public World world()                 { return world; }
    public SetupConfig config()          { return config; }
    public Notifications notifications() { return notifications; }
    public boolean helpVisible()         { return helpVisible; }
    public void toggleHelp()             { helpVisible = !helpVisible; }
    public String setupError()           { return setupError; }
    public boolean inPlay() {
        return state == State.PLAYING || state == State.PAUSED
            || state == State.PLACING_CASTLE || state == State.VICTORY
            || state == State.DEFEAT;
    }

    // --- transitions ---
    public void openMenu() {
        state = State.MENU;
        world = null;
        helpVisible = false;
    }

    public void openSetup() {
        setupError = null;
        state = State.SETUP;
    }

    /** Start a game from the current setup config; enter interactive founding. */
    public void startGame() {
        World w = new World(config.buildMap());
        if (!w.hasAnyCastleSite()) {
            setupError = "This map has no buildable Castle site — try another.";
            state = State.SETUP;
            return;
        }
        this.world = w;
        this.helpVisible = false;
        this.lastHumanBuildings = 0;
        this.wasUnderAttack = false;
        this.warnedNoTrees = false;
        this.state = State.PLACING_CASTLE;
        notifications.push("Choose where to found your Castle");
    }

    /** One-click default game (used by the menu's Quick Start). */
    public void quickStart() {
        openSetup();
        startGame();
    }

    /** Human clicked a tile during founding. Places the Castle and starts play. */
    public boolean tryFoundCastle(int x, int y) {
        if (state != State.PLACING_CASTLE || world == null) return false;
        if (!world.foundSettlementAt(x, y)) return false;
        world.spawnAiPlayers(config.aiAggressions());
        state = State.PLAYING;
        notifications.push("Castle founded — build your realm!");
        return true;
    }

    /** Toggle pause during play. */
    public void togglePause() {
        if (state == State.PLAYING) state = State.PAUSED;
        else if (state == State.PAUSED) state = State.PLAYING;
    }

    // --- per-tick ---
    public void update(double dtSeconds) {
        float dt = (float) dtSeconds;
        if (state == State.PLAYING) {
            world.update(dtSeconds);
            trackEvents();
            World.Outcome outcome = world.checkOutcome();
            if (outcome == World.Outcome.VICTORY) {
                state = State.VICTORY;
                notifications.push("VICTORY — every rival is vanquished!");
            } else if (outcome == World.Outcome.DEFEAT) {
                state = State.DEFEAT;
                notifications.push("DEFEAT — your Castle has fallen.");
            }
        }
        notifications.update(dt);
    }

    /** Derive coarse notifications by diffing world state (no engine coupling). */
    private void trackEvents() {
        Economy eco = world.economy();
        if (eco == null) return;

        int humanBuildings = 0;
        boolean noTrees = false;
        for (Building b : world.buildings().all()) {
            if (b.ownerId() != World.PLAYER_ID) continue;
            if (b.isFinished()) humanBuildings++;
            if ("no trees".equals(eco.statusOf(b))) noTrees = true;
        }
        if (humanBuildings > lastHumanBuildings && lastHumanBuildings > 0) {
            notifications.push("Building finished");
        }
        lastHumanBuildings = humanBuildings;

        if (noTrees && !warnedNoTrees) {
            notifications.push("Out of trees — build a Forester's Hut");
            warnedNoTrees = true;
        } else if (!noTrees) {
            warnedNoTrees = false;
        }

        boolean underAttack = world.military().incomingAttacksOn(World.PLAYER_ID) > 0;
        if (underAttack && !wasUnderAttack) {
            notifications.push("Under attack!");
        }
        wasUnderAttack = underAttack;
    }
}
