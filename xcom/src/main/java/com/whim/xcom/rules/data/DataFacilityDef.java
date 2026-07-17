package com.whim.xcom.rules.data;

import com.whim.xcom.rules.def.FacilityDef;

/** Immutable {@link FacilityDef} backed by data-pack fields. */
public final class DataFacilityDef implements FacilityDef {

    private String id;
    private String name;
    private int buildCostDollars;
    private int buildTimeDays;
    private int monthlyMaintenanceDollars;
    private int size = 1;
    private int detectionRange;
    private int detectionChancePercent;
    private int capacity;

    public DataFacilityDef(String id, String name, int buildCostDollars, int buildTimeDays,
                           int monthlyMaintenanceDollars, int size, int detectionRange,
                           int detectionChancePercent, int capacity) {
        this.id = id;
        this.name = name;
        this.buildCostDollars = buildCostDollars;
        this.buildTimeDays = buildTimeDays;
        this.monthlyMaintenanceDollars = monthlyMaintenanceDollars;
        this.size = size;
        this.detectionRange = detectionRange;
        this.detectionChancePercent = detectionChancePercent;
        this.capacity = capacity;
    }

    DataFacilityDef() {
    }

    @Override public String id() { return id; }
    @Override public String name() { return name; }
    @Override public int buildCostDollars() { return buildCostDollars; }
    @Override public int buildTimeDays() { return buildTimeDays; }
    @Override public int monthlyMaintenanceDollars() { return monthlyMaintenanceDollars; }
    @Override public int size() { return size; }
    @Override public int detectionRange() { return detectionRange; }
    @Override public int detectionChancePercent() { return detectionChancePercent; }
    @Override public int capacity() { return capacity; }
}
