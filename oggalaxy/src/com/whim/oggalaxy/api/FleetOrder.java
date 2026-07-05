package com.whim.oggalaxy.api;

import java.util.EnumMap;
import java.util.Map;

/**
 * A fleet dispatch request built by the fleet-dispatch dialog and handed to
 * {@link GameController#dispatchFleet(FleetOrder)}.
 */
public final class FleetOrder {

    public final String originPlanetId;
    public final int targetGalaxy;
    public final int targetSystem;
    public final int targetPosition;
    public final boolean targetMoon;
    public final Ids.MissionType mission;
    public final Map<Ids.ShipType, Integer> ships;
    public final Cost cargo;
    public final int speedPct;       // 10..100
    public final int holdTicks;      // expedition dwell time / deploy hold

    public FleetOrder(String originPlanetId, int targetGalaxy, int targetSystem, int targetPosition,
                      boolean targetMoon, Ids.MissionType mission, Map<Ids.ShipType, Integer> ships,
                      Cost cargo, int speedPct, int holdTicks) {
        this.originPlanetId = originPlanetId;
        this.targetGalaxy = targetGalaxy;
        this.targetSystem = targetSystem;
        this.targetPosition = targetPosition;
        this.targetMoon = targetMoon;
        this.mission = mission;
        this.ships = ships == null ? new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class) : ships;
        this.cargo = cargo == null ? Cost.ZERO : cargo;
        this.speedPct = speedPct <= 0 ? 100 : speedPct;
        this.holdTicks = holdTicks;
    }
}
