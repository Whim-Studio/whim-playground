package com.whim.merchantprince.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A single ship or caravan owned by a family (GAME_DESIGN_REFERENCE §4). A unit is
 * either docked at a city ({@link #locationCityId} set, {@link #destinationCityId}==-1)
 * or in transit toward a destination with {@link #turnsRemaining} legs left. An
 * optional {@link #route} of city ids lets a unit run an automated recurring trade
 * loop (a confirmed feature of the original).
 */
public class TransportUnit implements Serializable {
    private static final long serialVersionUID = 1L;

    public final int id;
    public final int ownerId;
    public final UnitType type;

    public String customName = "";

    /** Cargo held, indexed by {@link Good#ordinal()}. */
    public final int[] cargo = new int[Good.COUNT];

    /** City the unit is docked at, or the city it most recently departed. */
    public int locationCityId;
    /** Destination while in transit, or -1 when docked. */
    public int destinationCityId = -1;
    /** Whole turns remaining until arrival at {@link #destinationCityId}. */
    public int turnsRemaining = 0;

    /** Optional automated route: ordered city ids the unit cycles through. */
    public final List<Integer> route = new ArrayList<Integer>();
    public boolean autoRoute = false;
    public int routeIndex = 0;

    public TransportUnit(int id, int ownerId, UnitType type, int locationCityId) {
        this.id = id;
        this.ownerId = ownerId;
        this.type = type;
        this.locationCityId = locationCityId;
    }

    public boolean inTransit() { return destinationCityId >= 0; }

    public int cargoUsed() {
        int t = 0;
        for (int q : cargo) t += q;
        return t;
    }

    public int cargoFree() { return type.capacity - cargoUsed(); }

    public String displayName() {
        return customName.isEmpty() ? (type.label + " #" + id) : customName;
    }
}
