package com.railroad.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single locomotive assigned to one {@link Route}, shuttling back and forth.
 *
 * <p>Position is stored as a floating index into the route path: 0.0 is the
 * first tile, {@code route.segmentCount()} is the last. {@code direction} is +1
 * heading toward {@code to} and -1 heading back toward {@code from}. When it
 * reaches an endpoint it reverses; the game logic reads {@link #consumeArrival()}
 * to load/deliver cargo there.
 *
 * <p>Phase 2 puts {@code capacity} to work: the train holds up to {@code capacity}
 * {@link Cargo} carloads, loading at one endpoint and delivering demanded cargo
 * at the other. {@link #arrivedAtTo()} reports which endpoint the last arrival
 * was, so {@link GameState} knows which station to service.
 */
public final class Train {

    private final String name;
    private Route route;
    private double position;      // index along route.getPath()
    private int direction = 1;    // +1 toward 'to', -1 toward 'from'
    private double speed;         // path-indices advanced per in-game day
    private final int capacity;   // max carloads carried

    private final List<Cargo> hold = new ArrayList<Cargo>();

    private boolean arrived;      // set true on the tick an endpoint is reached
    private boolean arrivedAtTo;  // true if the last arrival was the 'to' endpoint

    public Train(String name, Route route, double speed, int capacity) {
        this.name = name;
        this.route = route;
        this.speed = speed;
        this.capacity = capacity;
        this.position = 0.0;
    }

    public String getName() {
        return name;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
        this.position = 0.0;
        this.direction = 1;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getPosition() {
        return position;
    }

    public int getDirection() {
        return direction;
    }

    /**
     * Advances the train by {@code days} of travel. Reverses at either endpoint
     * and flags an arrival there so revenue can be booked once per one-way trip.
     */
    public void advance(double days) {
        if (route == null) {
            return;
        }
        int end = route.segmentCount();
        if (end <= 0) {
            return;
        }
        position += direction * speed * days;
        if (position >= end) {
            position = end;
            direction = -1;
            arrived = true;
            arrivedAtTo = true;
        } else if (position <= 0) {
            position = 0;
            direction = 1;
            arrived = true;
            arrivedAtTo = false;
        }
    }

    /** Returns true exactly once per endpoint arrival, clearing the flag. */
    public boolean consumeArrival() {
        if (arrived) {
            arrived = false;
            return true;
        }
        return false;
    }

    /** Which endpoint the most recent arrival was: true = {@code to}, false = {@code from}. */
    public boolean arrivedAtTo() {
        return arrivedAtTo;
    }

    /** The town the most recent arrival reached. */
    public Town arrivalTown() {
        return arrivedAtTo ? route.getTo() : route.getFrom();
    }

    // --- Phase 2: cargo hold --------------------------------------------------

    /** Carloads currently aboard. */
    public int loadCount() {
        return hold.size();
    }

    /** True while there is room for at least one more carload. */
    public boolean hasSpace() {
        return hold.size() < capacity;
    }

    /** Loads one carload if there is room. */
    public boolean load(Cargo cargo) {
        if (!hasSpace()) {
            return false;
        }
        return hold.add(cargo);
    }

    /** Live, unmodifiable view of the hold (for the HUD). */
    public List<Cargo> getHold() {
        return Collections.unmodifiableList(hold);
    }

    /**
     * Removes and returns every carload whose type is in {@code demanded}. Used
     * by {@link GameState} to unload only what the arrival station will pay for.
     */
    public List<Cargo> unloadDemanded(java.util.Set<CargoType> demanded) {
        List<Cargo> out = new ArrayList<Cargo>();
        java.util.Iterator<Cargo> it = hold.iterator();
        while (it.hasNext()) {
            Cargo c = it.next();
            if (demanded.contains(c.getType())) {
                out.add(c);
                it.remove();
            }
        }
        return out;
    }
}
