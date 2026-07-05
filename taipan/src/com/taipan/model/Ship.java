package com.taipan.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * The player's lorcha: its cargo hold, guns and hull damage.
 *
 * Both cargo and guns consume hold space (each gun occupies
 * {@link GameConstants#GUN_HOLD_SPACE} units), exactly as in the original,
 * which forces a trade-off between firepower and carrying capacity.
 */
public class Ship {

    private int capacity;
    private int guns;
    private int damage; // 0..100 percent
    private final Map<Good, Integer> cargo = new EnumMap<Good, Integer>(Good.class);

    public Ship(int capacity, int guns) {
        this.capacity = capacity;
        this.guns = guns;
        this.damage = 0;
        for (Good g : Good.values()) {
            cargo.put(g, 0);
        }
    }

    public int getCapacity() {
        return capacity;
    }

    public void addCapacity(int amount) {
        capacity += amount;
    }

    public int getGuns() {
        return guns;
    }

    public void addGuns(int n) {
        guns = Math.max(0, guns + n);
    }

    public int getDamage() {
        return damage;
    }

    public void addDamage(int d) {
        damage = Math.min(GameConstants.MAX_DAMAGE, Math.max(0, damage + d));
    }

    public void repair(int amount) {
        damage = Math.max(0, damage - amount);
    }

    public boolean isSunk() {
        return damage >= GameConstants.MAX_DAMAGE;
    }

    public int getCargo(Good g) {
        return cargo.get(g);
    }

    public void addCargo(Good g, int qty) {
        cargo.put(g, Math.max(0, cargo.get(g) + qty));
    }

    /** Hold units occupied by cargo alone. */
    public int cargoUnits() {
        int total = 0;
        for (Good g : Good.values()) {
            total += cargo.get(g);
        }
        return total;
    }

    /** Total hold units used, including guns. */
    public int usedHold() {
        return cargoUnits() + guns * GameConstants.GUN_HOLD_SPACE;
    }

    public int freeHold() {
        return capacity - usedHold();
    }
}
