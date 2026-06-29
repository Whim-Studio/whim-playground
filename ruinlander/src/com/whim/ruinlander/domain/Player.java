package com.whim.ruinlander.domain;

import java.util.EnumMap;
import java.util.Map;

/** The player character: survival stats, inventory, equipped gear, position, AP, reputation. */
public class Player {
    private final Map<StatType, Integer> current = new EnumMap<StatType, Integer>(StatType.class);
    private final Map<StatType, Integer> max = new EnumMap<StatType, Integer>(StatType.class);
    private final Inventory inventory = new Inventory();
    private final Map<Faction, Integer> reputation = new EnumMap<Faction, Integer>(Faction.class);

    private Weapon equippedWeapon;
    private Armor equippedArmor;
    private Position position;
    private int actionPoints;

    public Player(Position start) {
        this.position = start;
        // Sensible starting maxima.
        for (StatType t : StatType.values()) {
            max.put(t, 100);
        }
        // Starting values: full health & comfortable temperature; other meters empty.
        current.put(StatType.HEALTH, 100);
        current.put(StatType.HUNGER, 0);
        current.put(StatType.THIRST, 0);
        current.put(StatType.FATIGUE, 0);
        current.put(StatType.RADIATION, 0);
        current.put(StatType.TEMPERATURE, 100);
        for (Faction f : Faction.values()) {
            reputation.put(f, 0);
        }
    }

    public int getStat(StatType t) {
        Integer v = current.get(t);
        return v == null ? 0 : v;
    }

    public int getMaxStat(StatType t) {
        Integer v = max.get(t);
        return v == null ? 0 : v;
    }

    public void setMaxStat(StatType t, int v) {
        max.put(t, Math.max(0, v));
        // Re-clamp current against new max.
        setStat(t, getStat(t));
    }

    /** Clamps {@code v} into [0, max] for the stat. */
    public void setStat(StatType t, int v) {
        int m = getMaxStat(t);
        int clamped = v;
        if (clamped < 0) clamped = 0;
        if (clamped > m) clamped = m;
        current.put(t, clamped);
    }

    public void addStat(StatType t, int delta) {
        setStat(t, getStat(t) + delta);
    }

    public boolean isDead() {
        return getStat(StatType.HEALTH) <= 0;
    }

    public Position getPosition() { return position; }
    public void setPosition(Position p) { this.position = p; }

    public Inventory getInventory() { return inventory; }

    public Weapon getEquippedWeapon() { return equippedWeapon; }
    public void equipWeapon(Weapon w) { this.equippedWeapon = w; }

    public Armor getEquippedArmor() { return equippedArmor; }
    public void equipArmor(Armor a) { this.equippedArmor = a; }

    public int getActionPoints() { return actionPoints; }
    public void setActionPoints(int ap) { this.actionPoints = Math.max(0, ap); }

    public int getReputation(Faction f) {
        Integer v = reputation.get(f);
        return v == null ? 0 : v;
    }

    public void addReputation(Faction f, int d) {
        reputation.put(f, getReputation(f) + d);
    }
}
