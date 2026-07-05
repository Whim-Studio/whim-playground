package com.whim.oggalaxy.model;

import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.api.Views;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A planet or moon owned by an empire. Holds building levels, stationed ships and
 * defenses, a {@link ResourceStore}, the current building construction and the shipyard
 * queue. Implements {@link Views.PlanetView}.
 */
public final class Planet implements Views.PlanetView, Serializable {

    private static final long serialVersionUID = 1L;

    public String id;
    public String name;
    public String ownerId;
    public int galaxy;
    public int system;
    public int position;
    public boolean moon;
    public boolean hasMoon;
    public int minTemp;
    public int maxTemp;
    public int fieldsMax;

    public final Map<Ids.BuildingType, Integer> buildings = new EnumMap<Ids.BuildingType, Integer>(Ids.BuildingType.class);
    public final Map<Ids.ShipType, Integer> ships = new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class);
    public final Map<Ids.DefenseType, Integer> defenses = new EnumMap<Ids.DefenseType, Integer>(Ids.DefenseType.class);

    public final ResourceStore res = new ResourceStore();
    public Job construction;                       // building/research shown here, or null
    public final List<Job> shipyard = new ArrayList<Job>();

    public Planet() {
    }

    public int buildingLevelOf(Ids.BuildingType t) {
        Integer v = buildings.get(t);
        return v == null ? 0 : v;
    }

    public int shipCountOf(Ids.ShipType t) {
        Integer v = ships.get(t);
        return v == null ? 0 : v;
    }

    public int defenseCountOf(Ids.DefenseType t) {
        Integer v = defenses.get(t);
        return v == null ? 0 : v;
    }

    public void addShips(Ids.ShipType t, int n) {
        if (n == 0) return;
        int v = shipCountOf(t) + n;
        if (v <= 0) ships.remove(t);
        else ships.put(t, v);
    }

    public void addDefense(Ids.DefenseType t, int n) {
        if (n == 0) return;
        int v = defenseCountOf(t) + n;
        if (v <= 0) defenses.remove(t);
        else defenses.put(t, v);
    }

    /** Used building fields = sum of all building levels (OGame convention). */
    public int usedFields() {
        int sum = 0;
        for (Integer v : buildings.values()) sum += v;
        return sum;
    }

    @Override public String id() { return id; }
    @Override public String name() { return name; }
    @Override public int galaxy() { return galaxy; }
    @Override public int system() { return system; }
    @Override public int position() { return position; }
    @Override public boolean isMoon() { return moon; }
    @Override public boolean hasMoon() { return hasMoon; }
    @Override public int minTemp() { return minTemp; }
    @Override public int maxTemp() { return maxTemp; }
    @Override public int fieldsUsed() { return usedFields(); }
    @Override public int fieldsMax() { return fieldsMax; }
    @Override public int buildingLevel(Ids.BuildingType type) { return buildingLevelOf(type); }
    @Override public int shipCount(Ids.ShipType type) { return shipCountOf(type); }
    @Override public int defenseCount(Ids.DefenseType type) { return defenseCountOf(type); }
    @Override public Views.ResourceView resources() { return res; }
    @Override public Views.QueueItemView currentConstruction() { return construction; }
    @Override public List<Views.QueueItemView> shipyardQueue() {
        return new ArrayList<Views.QueueItemView>(shipyard);
    }
}
