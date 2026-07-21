package com.railroad.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public static final double DEFAULT_TRAIN_SPEED = 0.9;  // path-indices per day
    public static final int DEFAULT_TRAIN_CAPACITY = 8;    // carloads a train can haul

    private final World world;
    private final TrackNetwork network;
    private final Company company;
    private final GameDate date;
    private final List<Train> trains = new ArrayList<Train>();
    private final List<Station> stations = new ArrayList<Station>();

    private long lastDeliveryRevenue; // for HUD feedback
    private long totalFreightRevenue; // running total (Phase 3 finance reads this)
    private int completedTrips;
    private final List<DeliveryRecord> deliveries = new ArrayList<DeliveryRecord>();

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

    public List<Station> getStations() {
        return Collections.unmodifiableList(stations);
    }

    /** The station occupying tile {@code p}, or null if none. */
    public Station stationAt(GridPoint p) {
        for (Station s : stations) {
            if (s.getPosition().equals(p)) {
                return s;
            }
        }
        return null;
    }

    public long getLastDeliveryRevenue() {
        return lastDeliveryRevenue;
    }

    public long getTotalFreightRevenue() {
        return totalFreightRevenue;
    }

    /** Immutable log of every delivery (for Phase 3 finance reporting). */
    public List<DeliveryRecord> getDeliveries() {
        return Collections.unmodifiableList(deliveries);
    }

    public int getCompletedTrips() {
        return completedTrips;
    }

    /**
     * Builds a station at {@code p}, charged to the company. Valid only on a town
     * tile or a tile adjacent to a town, when nothing is already there and funds
     * suffice.
     *
     * @return the new Station, or null if the placement was rejected.
     */
    public Station buildStation(GridPoint p) {
        if (!world.getGrid().inBounds(p) || stationAt(p) != null) {
            return null;
        }
        if (!isTownOrAdjacent(p)) {
            return null;
        }
        if (!company.canAfford(Economy.STATION_COST) || !company.spend(Economy.STATION_COST)) {
            return null;
        }
        Station station = new Station("Station " + (stations.size() + 1), p,
                world.getGrid(), world);
        stations.add(station);
        return station;
    }

    /** True if {@code p} is a town tile or Chebyshev-adjacent to one. */
    public boolean isTownOrAdjacent(GridPoint p) {
        for (Town t : world.getTowns()) {
            GridPoint tp = t.getPosition();
            if (tp.equals(p) || tp.isAdjacent(p)) {
                return true;
            }
        }
        return false;
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
     * Advances the simulation by {@code dDays} in-game days. This is the single
     * driver: first production accrues at every town and industry, then every
     * train moves; on reaching a station a train first delivers demanded cargo
     * for revenue and then loads whatever cargo the station has available.
     */
    public void tick(double dDays) {
        date.advance(dDays);

        // 1. Production accrues at sources.
        for (Town t : world.getTowns()) {
            t.produce(dDays);
        }
        for (Industry ind : world.getIndustries()) {
            ind.produce(dDays);
        }

        // 2. Trains move, then load/haul/deliver at endpoints.
        for (Train train : trains) {
            train.advance(dDays);
            if (train.consumeArrival()) {
                completedTrips++;
                serviceArrival(train);
            }
        }
    }

    /**
     * Delivers demanded cargo (for revenue) and then loads available cargo when a
     * train arrives at an endpoint that has a station.
     */
    private void serviceArrival(Train train) {
        Station station = stationAt(train.arrivalTown().getPosition());
        if (station == null) {
            return; // town has no station yet — nothing to service
        }
        deliverTo(train, station);
        loadFrom(train, station);
    }

    /** Unloads and pays for every carload the station demands. */
    private void deliverTo(Train train, Station station) {
        Set<CargoType> demanded = new HashSet<CargoType>();
        for (CargoType type : CargoType.values()) {
            if (station.demands(type)) {
                demanded.add(type);
            }
        }
        if (demanded.isEmpty()) {
            return;
        }
        List<Cargo> unloaded = train.unloadDemanded(demanded);
        int distance = train.getRoute().segmentCount();
        long tripRevenue = 0;
        for (Cargo c : unloaded) {
            long r = Economy.deliveryRevenue(c.getType(), distance);
            tripRevenue += r;
            station.receive(c.getType());
            deliveries.add(new DeliveryRecord(c.getType(), 1, distance, r, date.getElapsedDays()));
        }
        if (tripRevenue > 0) {
            company.earn(tripRevenue);
            lastDeliveryRevenue = tripRevenue;
            totalFreightRevenue += tripRevenue;
        }
    }

    /**
     * Fills the train's remaining capacity from the station's catchment,
     * round-robin across the available cargo types so no single busy source (a
     * town's passengers) crowds out the others (a mine's coal).
     */
    private void loadFrom(Train train, Station station) {
        while (train.hasSpace()) {
            List<CargoType> available = station.availableCargoTypes();
            if (available.isEmpty()) {
                break; // nothing left to load
            }
            boolean loadedAny = false;
            for (CargoType type : available) {
                if (!train.hasSpace()) {
                    break;
                }
                if (station.take(type)) {
                    train.load(new Cargo(type, station.getPosition()));
                    loadedAny = true;
                }
            }
            if (!loadedAny) {
                break;
            }
        }
    }
}
