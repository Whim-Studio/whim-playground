package com.whim.ruinlander.domain;

/** Top-level mutable game state: world, player, current mode, combat handle, turn count. */
public class GameStateManager {
    private final GridMap map;
    private final Player player;
    private GameMode mode = GameMode.EXPLORATION;
    private Object combatState; // engine.CombatState held as Object to avoid domain->engine import
    private int turnCount;

    public GameStateManager(GridMap map, Player player) {
        this.map = map;
        this.player = player;
    }

    public GridMap getMap() { return map; }
    public Player getPlayer() { return player; }

    public GameMode getMode() { return mode; }
    public void setMode(GameMode m) { this.mode = m; }

    /** Holds an {@code engine.CombatState}; the controller casts on retrieval. */
    public Object getCombatState() { return combatState; }
    public void setCombatState(Object cs) { this.combatState = cs; }

    public int getTurnCount() { return turnCount; }
    public void incrementTurn() { this.turnCount++; }

    /** Convenience transition: enter combat with the given engine combat-state handle. */
    public void enterCombat(Object cs) {
        this.combatState = cs;
        this.mode = GameMode.COMBAT;
    }

    /** Convenience transition: leave combat back to exploration. */
    public void endCombat() {
        this.combatState = null;
        if (this.mode == GameMode.COMBAT) {
            this.mode = GameMode.EXPLORATION;
        }
    }
}
