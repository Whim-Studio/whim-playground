package com.whim.ruinlander.engine;

import com.whim.ruinlander.domain.Enemy;
import com.whim.ruinlander.domain.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-encounter tactical state. Combat happens on its own small abstract grid
 * (separate from the overworld), so enemy positions here are combat-grid
 * coordinates assigned by {@link CombatEngine#start}. The player's overworld
 * position is left untouched; its combat-grid position lives here.
 *
 * <p>Held by {@code GameStateManager} as an opaque {@code Object} (the UI casts).
 */
public class CombatState {

    public static final int WIDTH = 10;
    public static final int HEIGHT = 7;

    private final List<Enemy> enemies;
    private final List<String> log = new ArrayList<String>();
    private Position playerPos;
    private boolean playerTurn = true;
    private int round = 1;

    public CombatState(List<Enemy> enemies, Position playerPos) {
        this.enemies = new ArrayList<Enemy>(enemies);
        this.playerPos = playerPos;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public List<Enemy> aliveEnemies() {
        List<Enemy> alive = new ArrayList<Enemy>();
        for (Enemy e : enemies) {
            if (!e.isDead()) {
                alive.add(e);
            }
        }
        return alive;
    }

    public Position getPlayerPos() {
        return playerPos;
    }

    public void setPlayerPos(Position p) {
        this.playerPos = p;
    }

    public boolean isPlayerTurn() {
        return playerTurn;
    }

    public void setPlayerTurn(boolean playerTurn) {
        this.playerTurn = playerTurn;
    }

    public int getRound() {
        return round;
    }

    public void nextRound() {
        round++;
    }

    public List<String> getLog() {
        return log;
    }

    public void log(String message) {
        log.add(message);
    }
}
