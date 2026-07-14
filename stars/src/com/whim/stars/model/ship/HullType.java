package com.whim.stars.model.ship;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.whim.stars.model.TechField;
import com.whim.stars.model.TechLevels;

/**
 * A ship hull: base mass/armor/fuel/cargo plus an ordered list of typed slots.
 * Hulls are shared catalogue entries; a {@link ShipDesign} wraps one hull and
 * fills its slots.
 */
public final class HullType implements Serializable {
    private static final long serialVersionUID = 1L;

    /** A single slot: the category it accepts and how many components it holds. */
    public static final class SlotDef implements Serializable {
        private static final long serialVersionUID = 1L;
        private final ComponentCategory category;
        private final int capacity;

        public SlotDef(ComponentCategory category, int capacity) {
            this.category = category;
            this.capacity = Math.max(1, capacity);
        }

        public ComponentCategory category() { return category; }
        public int capacity() { return capacity; }

        /** GENERAL slots accept anything; typed slots accept only their category. */
        public boolean accepts(ComponentCategory c) {
            return category == ComponentCategory.GENERAL || category == c;
        }
    }

    private final String name;
    private final int baseMass;
    private final int baseArmor;
    private final int fuelCapacity;
    private final int cargoCapacity;
    private final int ironium, boranium, germanium, resources; // build cost
    private final TechField techField;
    private final int techLevel;
    private final boolean starbase;
    private final List<SlotDef> slots;

    private HullType(Builder b) {
        this.name = b.name;
        this.baseMass = b.baseMass;
        this.baseArmor = b.baseArmor;
        this.fuelCapacity = b.fuelCapacity;
        this.cargoCapacity = b.cargoCapacity;
        this.ironium = b.ironium;
        this.boranium = b.boranium;
        this.germanium = b.germanium;
        this.resources = b.resources;
        this.techField = b.techField;
        this.techLevel = b.techLevel;
        this.starbase = b.starbase;
        this.slots = Collections.unmodifiableList(new ArrayList<SlotDef>(b.slots));
    }

    public String name() { return name; }
    public int baseMass() { return baseMass; }
    public int baseArmor() { return baseArmor; }
    public int fuelCapacity() { return fuelCapacity; }
    public int cargoCapacity() { return cargoCapacity; }
    public int ironium() { return ironium; }
    public int boranium() { return boranium; }
    public int germanium() { return germanium; }
    public int resources() { return resources; }
    public boolean isStarbase() { return starbase; }
    public List<SlotDef> slots() { return slots; }

    public boolean isAvailable(TechLevels tech) {
        return techField == null || tech.get(techField) >= techLevel;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static final class Builder {
        private final String name;
        private int baseMass = 20;
        private int baseArmor = 0;
        private int fuelCapacity = 0;
        private int cargoCapacity = 0;
        private int ironium, boranium, germanium, resources;
        private TechField techField;
        private int techLevel;
        private boolean starbase;
        private final List<SlotDef> slots = new ArrayList<SlotDef>();

        private Builder(String name) {
            this.name = name;
        }

        public Builder mass(int m) { this.baseMass = m; return this; }
        public Builder armor(int a) { this.baseArmor = a; return this; }
        public Builder fuel(int f) { this.fuelCapacity = f; return this; }
        public Builder cargo(int c) { this.cargoCapacity = c; return this; }
        public Builder cost(int ir, int bo, int ge, int res) {
            this.ironium = ir; this.boranium = bo; this.germanium = ge; this.resources = res; return this;
        }
        public Builder tech(TechField f, int level) { this.techField = f; this.techLevel = level; return this; }
        public Builder starbase(boolean b) { this.starbase = b; return this; }
        public Builder slot(ComponentCategory category, int capacity) {
            slots.add(new SlotDef(category, capacity));
            return this;
        }

        public HullType build() {
            return new HullType(this);
        }
    }
}
