package com.railroad.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * A production site on the map: a coal mine or a steel mill (see
 * {@link IndustryType}). Each industry holds a per-cargo stockpile measured in
 * fractional carloads; whole carloads (the floor of the stockpile) are what a
 * train can pick up or what an industry has "made".
 *
 * <p>Production is driven once per {@link GameState#tick}:
 * <ul>
 *   <li>A raw extractor (mine, {@code consumes == null}) accrues its product up
 *       to a cap every tick.</li>
 *   <li>A processor (mill) converts any input cargo it has been <em>delivered</em>
 *       into its product, up to a cap — it never conjures its input, so the
 *       chain only turns when coal actually arrives by rail.</li>
 * </ul>
 */
public final class Industry {

    private final int id;
    private final String name;
    private final IndustryType type;
    private final GridPoint position;

    /** Fractional-carload stockpiles keyed by cargo (both delivered input and made output). */
    private final Map<CargoType, Double> stock = new EnumMap<CargoType, Double>(CargoType.class);

    public Industry(int id, String name, IndustryType type, GridPoint position) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.position = position;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public IndustryType getType() {
        return type;
    }

    public GridPoint getPosition() {
        return position;
    }

    /** Whole carloads of {@code cargo} currently held. */
    public int carloadsOf(CargoType cargo) {
        return (int) Math.floor(get(cargo));
    }

    /** True if this industry turns {@code cargo} into its product. */
    public boolean consumes(CargoType cargo) {
        return type.getConsumes() == cargo;
    }

    /** The cargo type available for a train to pick up here. */
    public CargoType producedCargo() {
        return type.getProduces();
    }

    /** Whole carloads of the product available for pickup. */
    public int availableForPickup() {
        return carloadsOf(type.getProduces());
    }

    /** Removes one carload of the produced cargo (called when a train loads it). */
    public void takeOneProduced() {
        CargoType p = type.getProduces();
        set(p, Math.max(0.0, get(p) - 1.0));
    }

    /** Adds one delivered carload of an input cargo this industry consumes. */
    public void receiveInput(CargoType cargo) {
        if (consumes(cargo)) {
            set(cargo, Math.min(Economy.MILL_COAL_INPUT_CAP, get(cargo) + 1.0));
        }
    }

    /** Accrues raw product, or converts delivered input into product, for {@code dDays}. */
    public void produce(double dDays) {
        CargoType in = type.getConsumes();
        CargoType out = type.getProduces();
        if (in == null) {
            // Raw extractor: product simply accrues up to its cap.
            double cap = capFor(out);
            set(out, Math.min(cap, get(out) + rateFor(out) * dDays));
        } else {
            // Processor: convert only as much input as we actually hold.
            double want = Economy.MILL_CONVERT_RATE * dDays;
            double converted = Math.min(want, get(in));
            double room = capFor(out) - get(out);
            converted = Math.min(converted, Math.max(0.0, room));
            if (converted > 0) {
                set(in, get(in) - converted);
                set(out, get(out) + converted);
            }
        }
    }

    private double rateFor(CargoType c) {
        return c == CargoType.COAL ? Economy.MINE_COAL_RATE : Economy.MILL_CONVERT_RATE;
    }

    private double capFor(CargoType c) {
        return c == CargoType.STEEL ? Economy.MILL_STEEL_CAP : Economy.MINE_COAL_CAP;
    }

    private double get(CargoType c) {
        Double v = stock.get(c);
        return v == null ? 0.0 : v;
    }

    private void set(CargoType c, double v) {
        stock.put(c, v);
    }
}
