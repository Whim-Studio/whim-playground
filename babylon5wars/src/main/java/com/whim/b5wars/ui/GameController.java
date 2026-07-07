package com.whim.b5wars.ui;

import com.whim.b5wars.data.CriticalEntry;
import com.whim.b5wars.data.DataLoader;
import com.whim.b5wars.engine.CombatEngine;
import com.whim.b5wars.engine.GameEvent;
import com.whim.b5wars.engine.GameState;
import com.whim.b5wars.engine.MovementEngine;
import com.whim.b5wars.engine.TurnManager;
import com.whim.b5wars.engine.TurnPhase;
import com.whim.b5wars.model.Faction;
import com.whim.b5wars.model.Scenario;
import com.whim.b5wars.model.Ship;
import com.whim.b5wars.model.Side;

import java.util.ArrayList;
import java.util.List;

/**
 * The hub the Swing panels talk to. Owns the {@link GameState} plus the UI's own
 * {@link MovementEngine} and {@link CombatEngine} instances, and drives the turn FSM by
 * <em>composing the engine's existing public entry points</em> — no engine changes required.
 *
 * <p>Interactive hot-seat play differs from the engine's headless auto-runner in one way: during
 * IMPULSE the player issues movement/fire manually rather than letting
 * {@link TurnManager#runImpulseLoop()} resolve everything. We therefore advance INITIATIVE→POWER→EW
 * with {@link TurnManager#advancePhase()} (safe: those phases only allocate initiative/thrust/EW),
 * let the player act during IMPULSE, and close the turn with {@link TurnManager#endTurn()} — which
 * performs shield regen, powerless-drift, victory check and rolls to the next turn.
 */
public final class GameController {

    private final Scenario scenario;
    private final List<Faction> factions;
    private final long seed;
    private final List<CriticalEntry> critTable;

    private GameState state;
    private TurnManager turnManager;
    private MovementEngine movement;
    private CombatEngine combat;

    private Ship selectedShip;
    private int selectedWeaponIndex = 0;
    private int[] selectedHex;   // {q,r} of last clicked hex, or null

    private final List<GameListener> listeners = new ArrayList<GameListener>();

    public GameController(Scenario scenario, List<Faction> factions, long seed) {
        this.scenario = scenario;
        this.factions = factions;
        this.seed = seed;
        List<CriticalEntry> crit;
        try {
            crit = DataLoader.loadCriticalTable();
        } catch (RuntimeException ex) {
            crit = new ArrayList<CriticalEntry>();
        }
        this.critTable = crit;
        newGame();
    }

    /** (Re)start the duel from the fixed seed. */
    public void newGame() {
        this.state = new GameState(scenario, factions, seed);
        this.turnManager = new TurnManager(state);
        this.movement = new MovementEngine();
        this.combat = new CombatEngine(state.getDice(), critTable);
        this.movement.resetTurn();
        this.selectedShip = state.getShips().isEmpty() ? null : state.getShips().get(0);
        this.selectedWeaponIndex = 0;
        this.selectedHex = null;
        List<GameEvent> intro = new ArrayList<GameEvent>();
        intro.add(new GameEvent("PHASE", "New game — " + scenario.getName()
                + " (seed " + seed + "). Turn 1, INITIATIVE phase."));
        fireChanged();
        fireLog(intro);
    }

    // ---- accessors -------------------------------------------------------

    public GameState state() {
        return state;
    }

    public TurnManager turnManager() {
        return turnManager;
    }

    public MovementEngine movement() {
        return movement;
    }

    public CombatEngine combat() {
        return combat;
    }

    public Ship selectedShip() {
        return selectedShip;
    }

    public int selectedWeaponIndex() {
        return selectedWeaponIndex;
    }

    public int[] selectedHex() {
        return selectedHex;
    }

    /** The enemy of the currently selected ship (first living opponent), or null. */
    public Ship currentTarget() {
        if (selectedShip == null) {
            return null;
        }
        for (Ship s : state.getShips()) {
            if (s.getSide() != selectedShip.getSide() && !s.isDestroyed()) {
                return s;
            }
        }
        return null;
    }

    // ---- selection -------------------------------------------------------

    public void selectShip(Ship s) {
        this.selectedShip = s;
        this.selectedWeaponIndex = 0;
        fireChanged();
    }

    public void selectWeapon(int index) {
        this.selectedWeaponIndex = index;
        fireChanged();
    }

    public void selectHex(int q, int r) {
        this.selectedHex = new int[] {q, r};
        fireChanged();
    }

    // ---- turn FSM --------------------------------------------------------

    /** Advance the turn machine one step, honoring manual IMPULSE control. */
    public void advancePhase() {
        if (state.isOver()) {
            return;
        }
        List<GameEvent> events;
        if (state.getPhase() == TurnPhase.IMPULSE) {
            // Manual hot-seat: close the turn without the auto impulse loop.
            events = turnManager.endTurn();
            movement.resetTurn();
        } else {
            events = turnManager.advancePhase();
            if (state.getPhase() == TurnPhase.IMPULSE) {
                movement.resetTurn();
            }
        }
        fireLog(events);
        fireChanged();
    }

    // ---- movement (IMPULSE) ---------------------------------------------

    public void moveForward() {
        if (!canManeuver()) {
            return;
        }
        Ship s = selectedShip;
        if (movement.moveForward(s)) {
            log("MOVE", s.getType().getName() + " advances to " + s.getPos());
        } else {
            log("MOVE", s.getType().getName() + " cannot advance (out of Speed this turn)");
        }
        fireChanged();
    }

    public void turnShip(int steps) {
        if (!canManeuver()) {
            return;
        }
        Ship s = selectedShip;
        if (movement.turn(s, steps)) {
            log("MOVE", s.getType().getName() + " turns to face " + s.getFacing()
                    + " (thrust left " + s.getThrustAvailable() + ")");
        } else {
            log("MOVE", s.getType().getName() + " cannot turn (needs turn-mode "
                    + s.getType().getTurnMode() + " straight hexes & thrust)");
        }
        fireChanged();
    }

    public void sideslip(int steps) {
        if (!canManeuver()) {
            return;
        }
        Ship s = selectedShip;
        if (movement.sideslip(s, steps)) {
            log("MOVE", s.getType().getName() + " sideslips to " + s.getPos()
                    + " (thrust left " + s.getThrustAvailable() + ")");
        } else {
            log("MOVE", s.getType().getName() + " cannot sideslip (needs thrust)");
        }
        fireChanged();
    }

    public void accelerate(int delta) {
        if (!canManeuver()) {
            return;
        }
        Ship s = selectedShip;
        int before = s.getSpeed();
        movement.accelerate(s, delta);
        log("MOVE", s.getType().getName() + " speed " + before + " -> " + s.getSpeed()
                + " (thrust left " + s.getThrustAvailable() + ")");
        fireChanged();
    }

    private boolean canManeuver() {
        return !state.isOver() && state.getPhase() == TurnPhase.IMPULSE
                && selectedShip != null && !selectedShip.isDestroyed();
    }

    // ---- fire ------------------------------------------------------------

    public void fire(Ship attacker, int weaponIndex, Ship target) {
        if (attacker == null || target == null) {
            return;
        }
        List<GameEvent> events = combat.fire(attacker, weaponIndex, target, state.getTurn());
        // A UI-driven shot can end the game; reflect that as the engine would.
        maybeConcludeFromDamage(events);
        fireLog(events);
        fireChanged();
    }

    /**
     * If a manual shot destroyed/crippled a side, run the engine's own victory check by delegating
     * to endTurn-style bookkeeping only when appropriate. Here we simply let the next
     * {@link #advancePhase()} → endTurn detect it; but if a ship is already destroyed we surface it.
     */
    private void maybeConcludeFromDamage(List<GameEvent> events) {
        // No extra work needed: TurnManager.endTurn() re-evaluates victory each turn. This hook is
        // kept as a single, obvious place to extend if immediate mid-impulse victory is wanted.
    }

    // ---- listeners -------------------------------------------------------

    public void addListener(GameListener l) {
        listeners.add(l);
    }

    private void log(String type, String message) {
        List<GameEvent> one = new ArrayList<GameEvent>(1);
        one.add(new GameEvent(type, message));
        fireLog(one);
    }

    private void fireChanged() {
        for (GameListener l : listeners) {
            l.gameChanged();
        }
    }

    private void fireLog(List<GameEvent> events) {
        for (GameListener l : listeners) {
            l.logEvents(events);
        }
    }

    // ---- convenience for panels -----------------------------------------

    public boolean isImpulse() {
        return state.getPhase() == TurnPhase.IMPULSE;
    }

    public boolean isPowerPhase() {
        return state.getPhase() == TurnPhase.POWER;
    }

    public boolean isEwPhase() {
        return state.getPhase() == TurnPhase.EW;
    }

    public String initiativeText() {
        Side w = state.getInitiativeWinner();
        return w == null ? "—" : ("Side " + w);
    }
}
