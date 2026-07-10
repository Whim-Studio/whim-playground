package com.whim.scg.model;

import com.whim.scg.api.Enums;
import com.whim.scg.api.GridPos;
import com.whim.scg.api.Views;

import java.util.ArrayList;
import java.util.List;

/** Mutable ship: rooms, crew, weapons, hull/shields/reactor/oxygen state. */
public final class ShipModel implements Views.ShipView {
    public String name;
    public Enums.Faction faction;
    public int hull;
    public int maxHull;
    public int shields;
    public int maxShields;
    public int reactor;
    public double oxygen = 100;
    public int gridW, gridH;
    public final List<RoomModel> rooms = new ArrayList<RoomModel>();
    public final List<CrewModel> crew = new ArrayList<CrewModel>();
    public final List<WeaponModel> weapons = new ArrayList<WeaponModel>();
    /** Fractional shield regen accumulator. */
    public double shieldAccum;

    public RoomModel room(int id) {
        for (RoomModel r : rooms) if (r.id == id) return r;
        return null;
    }

    public RoomModel firstRoom(Enums.RoomType t) {
        for (RoomModel r : rooms) if (r.type == t) return r;
        return null;
    }

    public CrewModel crewById(int id) {
        for (CrewModel c : crew) if (c.id == id) return c;
        return null;
    }

    @Override public int reactorUsed() {
        int used = 0;
        for (RoomModel r : rooms) used += r.power;
        for (WeaponModel w : weapons) used += w.powered;
        return used;
    }

    public int reactorFree() { return reactor - reactorUsed(); }

    @Override public String name() { return name; }
    @Override public Enums.Faction faction() { return faction; }
    @Override public int hull() { return hull; }
    @Override public int maxHull() { return maxHull; }
    @Override public int shields() { return shields; }
    @Override public int maxShields() { return maxShields; }
    @Override public int reactor() { return reactor; }
    @Override public int oxygen() { return (int) Math.round(oxygen); }
    @Override public int gridW() { return gridW; }
    @Override public int gridH() { return gridH; }

    @Override public List<Views.RoomView> rooms() {
        List<Views.RoomView> out = new ArrayList<Views.RoomView>(rooms);
        return out;
    }
    @Override public List<Views.CrewView> crew() {
        List<Views.CrewView> out = new ArrayList<Views.CrewView>(crew);
        return out;
    }
    @Override public List<Views.WeaponView> weapons() {
        List<Views.WeaponView> out = new ArrayList<Views.WeaponView>(weapons);
        return out;
    }

    @Override public RoomModel roomAt(GridPos p) {
        if (p == null) return null;
        for (RoomModel r : rooms) if (r.contains(p)) return r;
        return null;
    }
}
