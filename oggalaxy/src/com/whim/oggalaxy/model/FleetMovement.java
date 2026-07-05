package com.whim.oggalaxy.model;

import com.whim.oggalaxy.api.Cost;
import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.api.Views;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

/**
 * A fleet in flight. Tracks its owner, mission, origin/target coordinates, ships, cargo
 * and the outbound / arrival / return tick schedule. The engine resolves the mission at
 * {@link #arrivalTick} then flips {@link #returning} and delivers the fleet home at
 * {@link #returnTick}. Implements {@link Views.FleetMovementView}.
 */
public final class FleetMovement implements Views.FleetMovementView, Serializable {

    private static final long serialVersionUID = 1L;

    public String id;
    public String ownerId;
    public String ownerName;
    public boolean player;
    public Ids.MissionType mission;
    public String originPlanetId;
    public int[] origin = new int[3];
    public int[] target = new int[3];
    public boolean targetMoon;
    public Map<Ids.ShipType, Integer> ships = new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class);
    public Cost cargo = Cost.ZERO;
    public int departTick;
    public int arrivalTick;
    public int returnTick;
    public int holdTicks;
    public int speedPct = 100;
    public boolean returning;
    public boolean resolvedAtTarget;   // mission already executed on arrival

    public FleetMovement() {
    }

    public int totalShips() {
        int n = 0;
        for (Integer v : ships.values()) n += v;
        return n;
    }

    @Override public String id() { return id; }
    @Override public String ownerName() { return ownerName; }
    @Override public boolean ownedByPlayer() { return player; }
    @Override public Ids.MissionType mission() { return mission; }
    @Override public int[] origin() { return origin; }
    @Override public int[] target() { return target; }
    @Override public Map<Ids.ShipType, Integer> ships() {
        return new EnumMap<Ids.ShipType, Integer>(ships);
    }
    @Override public Cost cargo() { return cargo; }
    @Override public int departTick() { return departTick; }
    @Override public int arrivalTick() { return arrivalTick; }
    @Override public int returnTick() { return returnTick; }
    @Override public boolean returning() { return returning; }
    @Override public String statusText() {
        if (returning) return "Returning";
        return mission + " → " + target[0] + ":" + target[1] + ":" + target[2];
    }
}
