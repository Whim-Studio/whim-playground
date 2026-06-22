package com.whim.civ.domain;

import java.util.ArrayList;
import java.util.List;

/** A city owned by a civilization. */
public final class City {
    private final int ownerCivId;
    private final String name;
    private final int x;
    private final int y;
    private int population = 1;
    private int foodStore;
    private int shieldStore;
    private final List<Building> buildings = new ArrayList<Building>();
    private UnitType producingUnit;
    private Building producingBuilding;
    private boolean inDisorder;

    public City(int ownerCivId, String name, int x, int y) {
        this.ownerCivId = ownerCivId;
        this.name = name;
        this.x = x;
        this.y = y;
    }

    public int getOwnerCivId() { return ownerCivId; }
    public String getName() { return name; }
    public int getX() { return x; }
    public int getY() { return y; }

    public int getPopulation() { return population; }    // # of citizens (size)
    public void setPopulation(int p) { this.population = p; }

    public int getFoodStore() { return foodStore; }
    public void setFoodStore(int f) { this.foodStore = f; }

    public int getShieldStore() { return shieldStore; }
    public void setShieldStore(int s) { this.shieldStore = s; }

    public List<Building> getBuildings() { return buildings; }   // completed buildings/wonders

    // Current production order: exactly one of these is non-null at a time.
    public UnitType getProducingUnit() { return producingUnit; }
    public void setProducingUnit(UnitType u) {
        this.producingUnit = u;
        if (u != null) {
            this.producingBuilding = null;
        }
    }

    public Building getProducingBuilding() { return producingBuilding; }
    public void setProducingBuilding(Building b) {
        this.producingBuilding = b;
        if (b != null) {
            this.producingUnit = null;
        }
    }

    public int getFoodBoxSize() { return (population + 1) * 10; }   // food needed to grow

    public boolean isInDisorder() { return inDisorder; }   // set by ENGINE when unhappy >= content
    public void setInDisorder(boolean d) { this.inDisorder = d; }
}
