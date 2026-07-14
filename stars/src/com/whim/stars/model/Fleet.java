package com.whim.stars.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.whim.stars.model.ship.ShipDesign;

/**
 * A fleet: a group of ships (counted per shared {@link ShipDesign}, matching how
 * Stars! stacks identical ships) at a position, with fuel, a cargo hold and an
 * ordered list of {@link Waypoint}s. Combat strength and movement derive from
 * the designs it carries.
 */
public final class Fleet implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private int ownerId;
    private String name;
    private double x;
    private double y;

    private long fuel;
    private final Cargo cargo = new Cargo();

    /** Ship counts keyed by design (LinkedHashMap keeps a stable display order). */
    private final Map<ShipDesign, Integer> ships = new LinkedHashMap<ShipDesign, Integer>();
    private final List<Waypoint> waypoints = new ArrayList<Waypoint>();

    public Fleet(int id, int ownerId, String name, double x, double y) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.x = x;
        this.y = y;
    }

    public int id() { return id; }
    public int ownerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; }

    public double x() { return x; }
    public double y() { return y; }
    public void setPosition(double x, double y) { this.x = x; this.y = y; }

    public long fuel() { return fuel; }
    public void setFuel(long fuel) { this.fuel = Math.max(0, Math.min(fuel, fuelCapacity())); }
    public Cargo cargo() { return cargo; }

    public Map<ShipDesign, Integer> ships() { return ships; }
    public List<Waypoint> waypoints() { return waypoints; }

    public void addShips(ShipDesign design, int count) {
        if (count <= 0) return;
        Integer current = ships.get(design);
        ships.put(design, (current == null ? 0 : current) + count);
    }

    public void removeShips(ShipDesign design, int count) {
        Integer current = ships.get(design);
        if (current == null) return;
        int remaining = current - count;
        if (remaining > 0) {
            ships.put(design, remaining);
        } else {
            ships.remove(design);
        }
    }

    public int shipCount() {
        int n = 0;
        for (int c : ships.values()) n += c;
        return n;
    }

    public boolean isEmpty() {
        return ships.isEmpty();
    }

    /** Total mass in kT: hulls + components + mineral & colonist cargo. */
    public long totalMass() {
        long mass = 0;
        for (Map.Entry<ShipDesign, Integer> e : ships.entrySet()) {
            mass += (long) e.getKey().totalMass() * e.getValue();
        }
        mass += cargo.mineralMass() + cargo.colonists();
        return mass;
    }

    /** Slowest safe warp across all ships (0 if the fleet has no engines). */
    public int maxWarp() {
        int warp = 0;
        boolean seen = false;
        for (ShipDesign d : ships.keySet()) {
            int w = d.maxWarp();
            warp = seen ? Math.min(warp, w) : w;
            seen = true;
        }
        return warp;
    }

    public long fuelCapacity() {
        long cap = 0;
        for (Map.Entry<ShipDesign, Integer> e : ships.entrySet()) {
            cap += (long) e.getKey().fuelCapacity() * e.getValue();
        }
        return cap;
    }

    public long cargoCapacity() {
        long cap = 0;
        for (Map.Entry<ShipDesign, Integer> e : ships.entrySet()) {
            cap += (long) e.getKey().cargoCapacity() * e.getValue();
        }
        return cap;
    }

    public int totalWeaponPower() {
        int power = 0;
        for (Map.Entry<ShipDesign, Integer> e : ships.entrySet()) {
            power += e.getKey().totalWeaponPower() * e.getValue();
        }
        return power;
    }

    public boolean isArmed() {
        return totalWeaponPower() > 0;
    }

    /** Combined hit points (armor + shields) of every ship — combat durability. */
    public long totalHitPoints() {
        long hp = 0;
        for (Map.Entry<ShipDesign, Integer> e : ships.entrySet()) {
            hp += (long) (e.getKey().totalArmor() + e.getKey().totalShield()) * e.getValue();
        }
        return hp;
    }

    public boolean canColonize() {
        for (ShipDesign d : ships.keySet()) {
            if (d.canColonize()) return true;
        }
        return false;
    }

    public boolean hasScanner() {
        for (ShipDesign d : ships.keySet()) {
            if (d.scanRange() > 0) return true;
        }
        return false;
    }

    public int scanRange() {
        int scan = 0;
        for (ShipDesign d : ships.keySet()) {
            scan = Math.max(scan, d.scanRange());
        }
        return scan;
    }

    /** The next waypoint to travel to, or null if the fleet is holding station. */
    public Waypoint nextWaypoint() {
        return waypoints.isEmpty() ? null : waypoints.get(0);
    }

    @Override
    public String toString() {
        return "Fleet#" + id + " " + name;
    }
}
