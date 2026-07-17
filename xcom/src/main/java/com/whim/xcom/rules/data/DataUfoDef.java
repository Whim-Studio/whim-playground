package com.whim.xcom.rules.data;

import com.whim.xcom.rules.def.UfoDef;

/** Immutable {@link UfoDef} backed by data-pack fields. */
public final class DataUfoDef implements UfoDef {

    private String id;
    private String name;
    private int hullPoints;
    private int speed;
    private int weaponPower;
    private int weaponRange;
    private int mapSize;
    private int minCrew;
    private int maxCrew;

    public DataUfoDef(String id, String name, int hullPoints, int speed, int weaponPower,
                      int weaponRange, int mapSize, int minCrew, int maxCrew) {
        this.id = id;
        this.name = name;
        this.hullPoints = hullPoints;
        this.speed = speed;
        this.weaponPower = weaponPower;
        this.weaponRange = weaponRange;
        this.mapSize = mapSize;
        this.minCrew = minCrew;
        this.maxCrew = maxCrew;
    }

    DataUfoDef() {
    }

    @Override public String id() { return id; }
    @Override public String name() { return name; }
    @Override public int hullPoints() { return hullPoints; }
    @Override public int speed() { return speed; }
    @Override public int weaponPower() { return weaponPower; }
    @Override public int weaponRange() { return weaponRange; }
    @Override public int mapSize() { return mapSize; }
    @Override public int minCrew() { return minCrew; }
    @Override public int maxCrew() { return maxCrew; }
}
