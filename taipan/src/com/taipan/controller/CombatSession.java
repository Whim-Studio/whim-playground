package com.taipan.controller;

import com.taipan.model.GameConstants;
import com.taipan.model.GameState;
import com.taipan.model.Good;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * An interactive sea battle against a pack of hostile ships. The view drives
 * it one round at a time via {@link #fight()}, {@link #run()} and
 * {@link #throwCargo(Good, int)}; each returns the narrative for that round.
 * When {@link #isOver()} becomes true the outcome is recorded on the flags.
 */
public class CombatSession {

    private final GameState state;
    private final Random rng;

    private final int initialShips;
    private int enemyShips;
    private int round = 0;

    private boolean over = false;
    private boolean escaped = false;
    private boolean victory = false;
    private boolean playerSunk = false;
    private long booty = 0L;

    public CombatSession(GameState state, int enemyShips) {
        this.state = state;
        this.rng = state.rng();
        this.initialShips = enemyShips;
        this.enemyShips = enemyShips;
    }

    public int getEnemyShips() {
        return enemyShips;
    }

    public int getInitialShips() {
        return initialShips;
    }

    public int getRound() {
        return round;
    }

    public boolean isOver() {
        return over;
    }

    public boolean isEscaped() {
        return escaped;
    }

    public boolean isVictory() {
        return victory;
    }

    public boolean isPlayerSunk() {
        return playerSunk;
    }

    public long getBooty() {
        return booty;
    }

    /** Attack with every gun aboard. */
    public List<String> fight() {
        List<String> log = new ArrayList<String>();
        round++;
        int guns = state.getShip().getGuns();
        if (guns <= 0) {
            log.add("You have no guns! You can only try to run.");
            enemyFire(log);
            return log;
        }
        int hits = 0;
        int sunk = 0;
        for (int i = 0; i < guns && enemyShips > 0; i++) {
            if (rng.nextDouble() < GameConstants.GUN_HIT_CHANCE) {
                hits++;
                if (rng.nextDouble() < GameConstants.GUN_SINK_CHANCE) {
                    sunk++;
                    enemyShips--;
                }
            }
        }
        log.add("You fire " + guns + " gun(s): " + hits + " hit(s), " + sunk + " ship(s) sunk.");

        // Some survivors may lose their nerve and flee.
        if (enemyShips > 0 && rng.nextDouble() < 0.15) {
            int fled = 1 + rng.nextInt(Math.max(1, enemyShips / 3 + 1));
            fled = Math.min(fled, enemyShips);
            enemyShips -= fled;
            log.add(fled + " ship(s) flee in terror!");
        }

        if (enemyShips <= 0) {
            winBattle(log);
            return log;
        }
        enemyFire(log);
        return log;
    }

    /** Attempt to flee; the chance improves each round the fight drags on. */
    public List<String> run() {
        List<String> log = new ArrayList<String>();
        round++;
        double damagePenalty = state.getShip().getDamage() / 200.0;
        double chance = 0.30 + 0.10 * round - damagePenalty;
        if (rng.nextDouble() < chance) {
            escaped = true;
            over = true;
            log.add("You slip away and escape the " + enemyShips + " hostile ship(s).");
            return log;
        }
        log.add("You can't escape them!");
        enemyFire(log);
        return log;
    }

    /** Jettison cargo to lighten ship and appease pursuers. */
    public List<String> throwCargo(Good good, int qty) {
        List<String> log = new ArrayList<String>();
        round++;
        int held = state.getShip().getCargo(good);
        qty = Math.min(qty, held);
        if (qty <= 0) {
            log.add("You have no " + good.display() + " to throw overboard.");
            enemyFire(log);
            return log;
        }
        state.getShip().addCargo(good, -qty);
        log.add("You heave " + qty + " unit(s) of " + good.display() + " into the sea.");
        // Chance the distraction lets some ships peel off.
        if (rng.nextDouble() < 0.40) {
            int left = 1 + rng.nextInt(Math.max(1, enemyShips / 2 + 1));
            left = Math.min(left, enemyShips);
            enemyShips -= left;
            log.add(left + " ship(s) break off to scavenge the cargo.");
            if (enemyShips <= 0) {
                escaped = true;
                over = true;
                log.add("The last of them gives up the chase. You are free.");
                return log;
            }
        }
        enemyFire(log);
        return log;
    }

    private void enemyFire(List<String> log) {
        if (over) {
            return;
        }
        int total = 0;
        for (int i = 0; i < enemyShips; i++) {
            total += GameConstants.ENEMY_DAMAGE_PER_SHIP_MIN
                    + rng.nextInt(GameConstants.ENEMY_DAMAGE_PER_SHIP_MAX
                    - GameConstants.ENEMY_DAMAGE_PER_SHIP_MIN + 1);
        }
        state.getShip().addDamage(total);
        log.add("The enemy fires! Your ship takes " + total + "% damage (now "
                + state.getShip().getDamage() + "%).");
        if (state.getShip().isSunk()) {
            playerSunk = true;
            over = true;
            state.setGameOver(true);
            log.add("Your ship is battered beneath the waves. All is lost.");
        }
    }

    private void winBattle(List<String> log) {
        victory = true;
        over = true;
        booty = (long) initialShips * GameConstants.BOOTY_PER_SHIP
                + rng.nextInt((int) GameConstants.BOOTY_PER_SHIP * 4);
        state.setCash(state.getCash() + booty);
        log.add("You have defeated them all! You salvage $" + booty + " in booty.");
    }
}
