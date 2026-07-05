package com.whim.oggalaxy.api;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Immutable definition of a building type: display data, cost scaling, energy/
 * production role, requirements and constraints. Runtime per-planet levels live in
 * the simulation's model; this describes the "kind" of building.
 */
public final class BuildingDef implements Serializable {

    private static final long serialVersionUID = 1L;

    public final Ids.BuildingType type;
    public final String name;
    public final String description;
    public final Cost baseCost;      // cost of level 1
    public final double costFactor;  // geometric growth per level
    /** Per-level base value interpreted by the engine according to the building role. */
    public final double roleBase;
    public final int maxLevel;       // 0 == unlimited
    public final boolean moonOnly;
    public final List<Requirement> requirements;

    public BuildingDef(Ids.BuildingType type, String name, String description,
                       Cost baseCost, double costFactor, double roleBase,
                       int maxLevel, boolean moonOnly, List<Requirement> requirements) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.baseCost = baseCost;
        this.costFactor = costFactor;
        this.roleBase = roleBase;
        this.maxLevel = maxLevel;
        this.moonOnly = moonOnly;
        this.requirements = requirements == null
                ? Collections.<Requirement>emptyList()
                : Collections.unmodifiableList(requirements);
    }

    /** Cost to upgrade from {@code currentLevel} to {@code currentLevel+1}. */
    public Cost costForNextLevel(int currentLevel) {
        return Formulas.levelCost(baseCost, costFactor, currentLevel + 1);
    }
}
