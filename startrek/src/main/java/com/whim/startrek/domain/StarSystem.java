package com.whim.startrek.domain;

import java.util.EnumMap;
import java.util.Map;

/**
 * A colonisable star system at a fixed grid location. Tracks ownership, population,
 * facility counts, and per-resource stockpiles and per-turn production.
 */
public class StarSystem {

    private final String name;
    private final int row;
    private final int col;

    private Race owner; // null = independent
    private long population;
    private boolean borgControlled;

    private final Map<FacilityType, Integer> facilities = new EnumMap<FacilityType, Integer>(FacilityType.class);
    private final Map<ResourceType, Long> stockpile = new EnumMap<ResourceType, Long>(ResourceType.class);
    private final Map<ResourceType, Long> production = new EnumMap<ResourceType, Long>(ResourceType.class);

    public StarSystem(String name, int row, int col) {
        this.name = name;
        this.row = row;
        this.col = col;
    }

    public String getName() {
        return name;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public Race getOwner() {
        return owner;
    }

    public void setOwner(Race r) {
        this.owner = r; // null = independent
    }

    public long getPopulation() {
        return population;
    }

    public void setPopulation(long p) {
        this.population = p;
    }

    /** Facility count, always >= 0. */
    public int getFacility(FacilityType t) {
        Integer n = facilities.get(t);
        return n == null ? 0 : n.intValue();
    }

    public void setFacility(FacilityType t, int count) {
        facilities.put(t, count < 0 ? 0 : count);
    }

    public long getStockpile(ResourceType r) {
        Long n = stockpile.get(r);
        return n == null ? 0L : n.longValue();
    }

    public void setStockpile(ResourceType r, long amt) {
        stockpile.put(r, amt);
    }

    /** Per-turn output of the given resource. */
    public long getProduction(ResourceType r) {
        Long n = production.get(r);
        return n == null ? 0L : n.longValue();
    }

    public void setProduction(ResourceType r, long amt) {
        production.put(r, amt);
    }

    public boolean isBorgControlled() {
        return borgControlled;
    }

    public void setBorgControlled(boolean b) {
        this.borgControlled = b;
    }
}
