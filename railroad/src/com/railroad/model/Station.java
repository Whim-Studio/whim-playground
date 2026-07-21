package com.railroad.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A station the player builds at (or next to) a town. Its <em>catchment</em> is
 * the set of tiles within {@link Economy#STATION_RADIUS} (Chebyshev) of the
 * station tile, clipped to the map. Any {@link Town} or {@link Industry} whose
 * tile lies inside the catchment is "served" by this station: the station both
 * gathers that entity's produced cargo for pickup and accepts (demands) the
 * cargo that entity consumes.
 *
 * <p>The station is the sole broker between trains and the world's producers and
 * consumers — trains never touch towns/industries directly, they load from and
 * deliver to a station.
 */
public final class Station {

    private final String name;
    private final GridPoint position;
    private final Set<GridPoint> catchment;      // tiles within radius, clipped to map
    private final List<Town> servedTowns = new ArrayList<Town>();
    private final List<Industry> servedIndustries = new ArrayList<Industry>();

    public Station(String name, GridPoint position, TileGrid grid, World world) {
        this.name = name;
        this.position = position;
        this.catchment = computeCatchment(position, grid);
        indexServed(world);
    }

    private static Set<GridPoint> computeCatchment(GridPoint centre, TileGrid grid) {
        Set<GridPoint> tiles = new HashSet<GridPoint>();
        int r = Economy.STATION_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                int x = centre.x + dx;
                int y = centre.y + dy;
                if (grid.inBounds(x, y)) {
                    tiles.add(new GridPoint(x, y));
                }
            }
        }
        return tiles;
    }

    /** Records which towns/industries fall inside the catchment. */
    private void indexServed(World world) {
        for (Town t : world.getTowns()) {
            if (catchment.contains(t.getPosition())) {
                servedTowns.add(t);
            }
        }
        for (Industry ind : world.getIndustries()) {
            if (catchment.contains(ind.getPosition())) {
                servedIndustries.add(ind);
            }
        }
    }

    public String getName() {
        return name;
    }

    public GridPoint getPosition() {
        return position;
    }

    /** Unmodifiable view of the catchment tiles (for the map overlay). */
    public Set<GridPoint> getCatchment() {
        return Collections.unmodifiableSet(catchment);
    }

    public List<Town> getServedTowns() {
        return Collections.unmodifiableList(servedTowns);
    }

    public List<Industry> getServedIndustries() {
        return Collections.unmodifiableList(servedIndustries);
    }

    /** True if any served town or industry falls in the catchment. */
    public boolean servesAnything() {
        return !servedTowns.isEmpty() || !servedIndustries.isEmpty();
    }

    /**
     * True if this station demands {@code cargo}: a served town wants
     * passengers/mail/steel, or a served industry consumes it as a raw input.
     */
    public boolean demands(CargoType cargo) {
        for (Town t : servedTowns) {
            if (t.demands(cargo)) {
                return true;
            }
        }
        for (Industry ind : servedIndustries) {
            if (ind.consumes(cargo)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The distinct cargo types with at least one carload ready to load right now,
     * across every served town and industry. Callers round-robin over this list
     * so a busy town's passengers don't starve out the freight a mine offers.
     */
    public List<CargoType> availableCargoTypes() {
        List<CargoType> types = new ArrayList<CargoType>();
        for (Town t : servedTowns) {
            addIfAbsentAvailable(types, t, CargoType.PASSENGERS);
            addIfAbsentAvailable(types, t, CargoType.MAIL);
        }
        for (Industry ind : servedIndustries) {
            if (ind.availableForPickup() >= 1 && !types.contains(ind.producedCargo())) {
                types.add(ind.producedCargo());
            }
        }
        return types;
    }

    private void addIfAbsentAvailable(List<CargoType> types, Town t, CargoType c) {
        if (t.availableCarloads(c) >= 1 && !types.contains(c)) {
            types.add(c);
        }
    }

    /**
     * Removes one carload of {@code type} from a served source, if any is
     * available, and returns whether a carload was taken.
     */
    public boolean take(CargoType type) {
        for (Town t : servedTowns) {
            if (t.availableCarloads(type) >= 1) {
                t.takeCarload(type);
                return true;
            }
        }
        for (Industry ind : servedIndustries) {
            if (ind.producedCargo() == type && ind.availableForPickup() >= 1) {
                ind.takeOneProduced();
                return true;
            }
        }
        return false;
    }

    /**
     * Accepts one delivered carload of a demanded cargo. If a served industry
     * consumes it (e.g. a mill taking coal), the carload is fed into that
     * industry's input so the chain can turn; otherwise it is simply consumed by
     * a town. Callers should only deliver cargo for which {@link #demands} is
     * true.
     */
    public void receive(CargoType cargo) {
        for (Industry ind : servedIndustries) {
            if (ind.consumes(cargo)) {
                ind.receiveInput(cargo);
                return;
            }
        }
        // Consumed by a town (passengers/mail/steel) — no further routing needed.
    }
}
