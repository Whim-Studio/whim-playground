package com.railroad.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * A town on the map. Phase 2 fills in the cargo supply/demand hooks Phase 1 left
 * reserved: a town <em>supplies</em> PASSENGERS and MAIL, which accrue over time
 * up to a cap ({@link #produce}), and <em>demands</em> PASSENGERS, MAIL and STEEL
 * (people travel/post between towns, and towns consume the manufactured good).
 *
 * <p>Supply is held as fractional carloads; a train only ever loads whole
 * carloads (the floor).
 */
public final class Town {

    private final int id;
    private final String name;
    private final GridPoint position;

    /** Fractional-carload supply of the cargoes this town produces. */
    private final Map<CargoType, Double> supply = new EnumMap<CargoType, Double>(CargoType.class);

    public Town(int id, String name, GridPoint position) {
        this.id = id;
        this.name = name;
        this.position = position;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public GridPoint getPosition() {
        return position;
    }

    public int getX() {
        return position.x;
    }

    public int getY() {
        return position.y;
    }

    // --- Phase 2: cargo supply / demand --------------------------------------

    /** True if this town consumes {@code cargo} (passengers, mail, or steel). */
    public boolean demands(CargoType cargo) {
        return cargo == CargoType.PASSENGERS
                || cargo == CargoType.MAIL
                || cargo == CargoType.STEEL;
    }

    /** Whole carloads of {@code cargo} currently available for pickup. */
    public int availableCarloads(CargoType cargo) {
        return (int) Math.floor(get(cargo));
    }

    /** Removes one carload of {@code cargo} (called when a train loads it). */
    public void takeCarload(CargoType cargo) {
        set(cargo, Math.max(0.0, get(cargo) - 1.0));
    }

    /** Accrues passengers and mail for {@code dDays}, each capped. */
    public void produce(double dDays) {
        set(CargoType.PASSENGERS, Math.min(Economy.TOWN_SUPPLY_CAP,
                get(CargoType.PASSENGERS) + Economy.TOWN_PASSENGER_RATE * dDays));
        set(CargoType.MAIL, Math.min(Economy.TOWN_SUPPLY_CAP,
                get(CargoType.MAIL) + Economy.TOWN_MAIL_RATE * dDays));
    }

    private double get(CargoType c) {
        Double v = supply.get(c);
        return v == null ? 0.0 : v;
    }

    private void set(CargoType c, double v) {
        supply.put(c, v);
    }
}
