package com.whim.oggalaxy.api;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Immutable definition of a ship class: combat stats, logistics stats, cost,
 * requirements and its rapid-fire table.
 *
 * Combat base stats are BEFORE technology multipliers:
 *   effective attack  = weapon  * (1 + 0.1*weaponsTech)
 *   effective shield  = shield  * (1 + 0.1*shieldTech)
 *   effective hull    = hull    * (1 + 0.1*armourTech)
 */
public final class ShipDef implements Serializable {

    private static final long serialVersionUID = 1L;

    public final Ids.ShipType type;
    public final String name;
    public final String description;

    public final double weapon;   // base attack per unit
    public final double shield;   // base shield per unit
    public final double hull;     // base hull (structural) points per unit

    public final double cargo;    // cargo capacity
    public final double speed;    // base speed
    public final double fuel;     // deuterium consumption base per unit

    public final Cost cost;
    public final boolean civil;   // true for cargo/colony/probe/recycler/satellite (not a warship)

    public final List<Requirement> requirements;
    /** Rapid fire: how many extra shots this ship gets against a given target type (OGame table). */
    public final Map<Ids.ShipType, Integer> rapidFireVsShips;
    public final Map<Ids.DefenseType, Integer> rapidFireVsDefense;

    public ShipDef(Ids.ShipType type, String name, String description,
                   double weapon, double shield, double hull,
                   double cargo, double speed, double fuel,
                   Cost cost, boolean civil,
                   List<Requirement> requirements,
                   Map<Ids.ShipType, Integer> rapidFireVsShips,
                   Map<Ids.DefenseType, Integer> rapidFireVsDefense) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.weapon = weapon;
        this.shield = shield;
        this.hull = hull;
        this.cargo = cargo;
        this.speed = speed;
        this.fuel = fuel;
        this.cost = cost;
        this.civil = civil;
        this.requirements = requirements == null
                ? Collections.<Requirement>emptyList()
                : Collections.unmodifiableList(requirements);
        this.rapidFireVsShips = rapidFireVsShips == null
                ? Collections.<Ids.ShipType, Integer>emptyMap()
                : Collections.unmodifiableMap(rapidFireVsShips);
        this.rapidFireVsDefense = rapidFireVsDefense == null
                ? Collections.<Ids.DefenseType, Integer>emptyMap()
                : Collections.unmodifiableMap(rapidFireVsDefense);
    }
}
