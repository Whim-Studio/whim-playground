package com.whim.stars.model.ship;

import java.io.Serializable;

import com.whim.stars.model.Cargo;
import com.whim.stars.model.TechField;
import com.whim.stars.model.TechLevels;

/**
 * An immutable ship-part definition (an engine, weapon, armor plate, scanner,
 * etc.). Instances are shared catalogue entries referenced by {@link ShipDesign}
 * slots. Built with the fluent {@link Builder} so a catalogue entry reads as a
 * short spec.
 *
 * <p>The catalogue itself is deliberately small in this build (see
 * {@link Catalogue}); the full Stars! component tables are Phase-2+ data.
 */
public final class Component implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final ComponentCategory category;

    // Build cost.
    private final int ironium;
    private final int boranium;
    private final int germanium;
    private final int resources;
    private final int mass;

    // Tech gate.
    private final TechField techField;
    private final int techLevel;

    // Stat block (0 where not applicable to the category).
    private final int armor;
    private final int shield;
    private final int maxWarp;       // engines
    private final int fuelCapacity;  // engines / tanks
    private final int cargoCapacity; // cargo pods / freighter parts
    private final int scanRange;     // scanners
    private final int weaponPower;   // weapons/bombs
    private final int weaponRange;   // weapons

    private Component(Builder b) {
        this.name = b.name;
        this.category = b.category;
        this.ironium = b.ironium;
        this.boranium = b.boranium;
        this.germanium = b.germanium;
        this.resources = b.resources;
        this.mass = b.mass;
        this.techField = b.techField;
        this.techLevel = b.techLevel;
        this.armor = b.armor;
        this.shield = b.shield;
        this.maxWarp = b.maxWarp;
        this.fuelCapacity = b.fuelCapacity;
        this.cargoCapacity = b.cargoCapacity;
        this.scanRange = b.scanRange;
        this.weaponPower = b.weaponPower;
        this.weaponRange = b.weaponRange;
    }

    public String name() { return name; }
    public ComponentCategory category() { return category; }
    public int ironium() { return ironium; }
    public int boranium() { return boranium; }
    public int germanium() { return germanium; }
    public int resources() { return resources; }
    public int mass() { return mass; }
    public TechField techField() { return techField; }
    public int techLevel() { return techLevel; }
    public int armor() { return armor; }
    public int shield() { return shield; }
    public int maxWarp() { return maxWarp; }
    public int fuelCapacity() { return fuelCapacity; }
    public int cargoCapacity() { return cargoCapacity; }
    public int scanRange() { return scanRange; }
    public int weaponPower() { return weaponPower; }
    public int weaponRange() { return weaponRange; }

    /** True if the given tech meets this component's field/level requirement. */
    public boolean isAvailable(TechLevels tech) {
        return techField == null || tech.get(techField) >= techLevel;
    }

    public Cargo mineralCost() {
        return new Cargo(ironium, boranium, germanium, 0, 0);
    }

    @Override
    public String toString() {
        return name;
    }

    public static Builder builder(String name, ComponentCategory category) {
        return new Builder(name, category);
    }

    /** Fluent builder — only the fields relevant to a component need be set. */
    public static final class Builder {
        private final String name;
        private final ComponentCategory category;
        private int ironium, boranium, germanium, resources, mass;
        private TechField techField;
        private int techLevel;
        private int armor, shield, maxWarp, fuelCapacity, cargoCapacity, scanRange, weaponPower, weaponRange;

        private Builder(String name, ComponentCategory category) {
            this.name = name;
            this.category = category;
        }

        public Builder cost(int ir, int bo, int ge, int res) {
            this.ironium = ir; this.boranium = bo; this.germanium = ge; this.resources = res; return this;
        }
        public Builder mass(int m) { this.mass = m; return this; }
        public Builder tech(TechField f, int level) { this.techField = f; this.techLevel = level; return this; }
        public Builder armor(int a) { this.armor = a; return this; }
        public Builder shield(int s) { this.shield = s; return this; }
        public Builder engine(int maxWarp, int fuelCapacity) { this.maxWarp = maxWarp; this.fuelCapacity = fuelCapacity; return this; }
        public Builder fuel(int cap) { this.fuelCapacity = cap; return this; }
        public Builder cargo(int cap) { this.cargoCapacity = cap; return this; }
        public Builder scan(int range) { this.scanRange = range; return this; }
        public Builder weapon(int power, int range) { this.weaponPower = power; this.weaponRange = range; return this; }

        public Component build() {
            return new Component(this);
        }
    }
}
