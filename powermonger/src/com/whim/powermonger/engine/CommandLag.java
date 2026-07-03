package com.whim.powermonger.engine;

/**
 * Command-lag math (carrier pigeons). Pure functions — depends on nothing but
 * primitives. Orders to a subordinate captain are delayed by a pigeon whose flight
 * time is proportional to the 2D Euclidean distance from the supreme commander
 * (id 0) to the target captain.
 */
public final class CommandLag {
    private CommandLag() {}

    /** Ticks of pigeon-flight per tile of distance. ~20 ticks/sec => ~0.4s / tile. */
    public static final double TICKS_PER_TILE = 8.0;

    /** Minimum flight duration in ticks, so even short hops show a pigeon briefly. */
    public static final int MIN_FLIGHT_TICKS = 6;

    /** 2D Euclidean distance between two fractional-tile points. */
    public static double distance(double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Total flight ticks for a pigeon covering the given distance. Proportional to
     * distance, floored at {@link #MIN_FLIGHT_TICKS}.
     */
    public static int flightTicks(double distance) {
        int ticks = (int) Math.ceil(distance * TICKS_PER_TILE);
        return Math.max(MIN_FLIGHT_TICKS, ticks);
    }

    /**
     * Per-tick progress increment (added to a 0..1 accumulator) for a flight of the
     * given total ticks. The order applies when the accumulator reaches 1.0.
     */
    public static double progressPerTick(int totalFlightTicks) {
        if (totalFlightTicks <= 0) {
            return 1.0;
        }
        return 1.0 / totalFlightTicks;
    }
}
