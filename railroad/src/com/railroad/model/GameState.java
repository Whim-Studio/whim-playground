package com.railroad.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central mutable game state and the single source of truth tying the static
 * {@link World} to the player's {@link TrackNetwork}, {@link Company} and
 * trains, plus the {@link GameDate} clock.
 *
 * <p>{@link #tick(double)} is the one place time advances: it moves every train
 * along its route and books trip revenue. The Swing timer (see logic.GameClock)
 * is the only caller in Phase 1.
 */
public final class GameState {

    // Balance constants. Tuned so Phase 1 reads clearly; not meant to mirror the
    // original game's economy.
    public static final long STARTING_CASH = 1_000_000L;
    public static final long TRAIN_COST = 40_000L;
    public static final long REVENUE_PER_SEGMENT = 1_200L; // per one-way trip
    public static final double DEFAULT_TRAIN_SPEED = 0.9;  // path-indices per day
    public static final int DEFAULT_TRAIN_CAPACITY = 8;    // reserved for Phase 2

    private final World world;
    private final TrackNetwork network;
    private final Company company;
    private final GameDate date;
    private final List<Train> trains = new ArrayList<Train>();

    private long lastTripRevenue; // for HUD feedback
    private int completedTrips;

    public GameState(World world, Company company) {
        this.world = world;
        this.company = company;
        this.network = new TrackNetwork();
        this.date = new GameDate(1830);
    }

    public World getWorld() {
        return world;
    }

    public TrackNetwork getNetwork() {
        return network;
    }

    public Company getCompany() {
        return company;
    }

    public GameDate getDate() {
        return date;
    }

    public List<Train> getTrains() {
        return Collections.unmodifiableList(trains);
    }

    public boolean hasTrain() {
        return !trains.isEmpty();
    }

    public long getLastTripRevenue() {
        return lastTripRevenue;
    }

    public int getCompletedTrips() {
        return completedTrips;
    }

    /**
     * Attempts to lay one track segment between two adjacent tiles. Cost is the
     * segment cost of the terrain being entered (the destination tile).
     *
     * @return true if the segment was built and paid for.
     */
    public boolean layTrack(GridPoint a, GridPoint b) {
        TileGrid grid = world.getGrid();
        if (!grid.inBounds(a) || !grid.inBounds(b) || !a.isAdjacent(b)) {
            return false;
        }
        if (network.hasSegmentBetween(a, b)) {
            return false; // already built — nothing to charge
        }
        int cost = segmentCost(a, b);
        if (!company.canAfford(cost)) {
            return false;
        }
        if (company.spend(cost)) {
            return network.addSegment(new TrackSegment(a, b, cost));
        }
        return false;
    }

    /** Cost to lay a segment between adjacent tiles: max of the two terrains. */
    public int segmentCost(GridPoint a, GridPoint b) {
        TileGrid grid = world.getGrid();
        int ca = grid.tileAt(a).getTerrain().getSegmentCost();
        int cb = grid.tileAt(b).getTerrain().getSegmentCost();
        return Math.max(ca, cb);
    }

    /**
     * Buys the single Phase 1 train and assigns it a route between two connected
     * towns. Fails if a train already exists, funds are short, or no path exists.
     *
     * @return the new Train, or null on failure.
     */
    public Train buyTrain(Town from, Town to) {
        if (hasTrain() || !company.canAfford(TRAIN_COST)) {
            return null;
        }
        List<GridPoint> path = network.findPath(from.getPosition(), to.getPosition());
        if (path == null || path.size() < 2) {
            return null;
        }
        if (!company.spend(TRAIN_COST)) {
            return null;
        }
        Route route = new Route(from, to, path);
        Train train = new Train("Loco 1", route, DEFAULT_TRAIN_SPEED, DEFAULT_TRAIN_CAPACITY);
        trains.add(train);
        return train;
    }

    /**
     * Advances the simulation by {@code dDays} in-game days: moves the clock and
     * every train, booking revenue for each completed one-way trip.
     */
    public void tick(double dDays) {
        date.advance(dDays);
        for (Train train : trains) {
            train.advance(dDays);
            if (train.consumeArrival()) {
                long revenue = (long) train.getRoute().segmentCount() * REVENUE_PER_SEGMENT;
                company.earn(revenue);
                lastTripRevenue = revenue;
                completedTrips++;
            }
        }
    }
}
