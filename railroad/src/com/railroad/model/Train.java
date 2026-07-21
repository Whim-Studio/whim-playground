package com.railroad.model;

/**
 * A single locomotive assigned to one {@link Route}, shuttling back and forth.
 *
 * <p>Position is stored as a floating index into the route path: 0.0 is the
 * first tile, {@code route.segmentCount()} is the last. {@code direction} is +1
 * heading toward {@code to} and -1 heading back toward {@code from}. When it
 * reaches an endpoint it reverses; the game logic reads {@link #consumeArrival()}
 * to award trip revenue.
 *
 * <p>{@code capacity} is unused in Phase 1 but present so Phase 2 cargo hauling
 * extends this class rather than replacing it.
 */
public final class Train {

    private final String name;
    private Route route;
    private double position;      // index along route.getPath()
    private int direction = 1;    // +1 toward 'to', -1 toward 'from'
    private double speed;         // path-indices advanced per in-game day
    private final int capacity;   // cargo capacity — reserved for Phase 2

    private boolean arrived;      // set true on the tick an endpoint is reached

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
        } else if (position <= 0) {
            position = 0;
            direction = 1;
            arrived = true;
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
}
