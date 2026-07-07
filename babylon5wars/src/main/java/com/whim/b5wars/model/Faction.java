package com.whim.b5wars.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A race and the ship classes it can field. */
public final class Faction {
    private final Race race;
    private final List<ShipClass> shipClasses;

    public Faction(Race race, List<ShipClass> shipClasses) {
        this.race = race;
        this.shipClasses = Collections.unmodifiableList(
                new ArrayList<ShipClass>(shipClasses == null ? new ArrayList<ShipClass>() : shipClasses));
    }

    public Race getRace() {
        return race;
    }

    public List<ShipClass> getShipClasses() {
        return shipClasses;
    }

    /** Lookup a ship class by id; null if absent. */
    public ShipClass byId(String id) {
        if (id == null) {
            return null;
        }
        for (ShipClass sc : shipClasses) {
            if (id.equals(sc.getId())) {
                return sc;
            }
        }
        return null;
    }
}
