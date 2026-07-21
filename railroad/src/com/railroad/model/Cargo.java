package com.railroad.model;

/**
 * One carload riding on a {@link Train}: its {@link CargoType} plus the position
 * of the station it was loaded at, so the distance it has been carried can be
 * priced on delivery (see {@link Economy#deliveryRevenue}).
 */
public final class Cargo {

    private final CargoType type;
    private final GridPoint origin;

    public Cargo(CargoType type, GridPoint origin) {
        this.type = type;
        this.origin = origin;
    }

    public CargoType getType() {
        return type;
    }

    /** Station position where this carload was loaded. */
    public GridPoint getOrigin() {
        return origin;
    }
}
