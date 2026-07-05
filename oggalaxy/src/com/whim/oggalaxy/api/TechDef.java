package com.whim.oggalaxy.api;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Immutable definition of a research technology.
 */
public final class TechDef implements Serializable {

    private static final long serialVersionUID = 1L;

    public final Ids.TechType type;
    public final String name;
    public final String description;
    public final Cost baseCost;
    public final double costFactor;
    public final int maxLevel;       // 0 == unlimited
    public final List<Requirement> requirements;

    public TechDef(Ids.TechType type, String name, String description,
                   Cost baseCost, double costFactor, int maxLevel, List<Requirement> requirements) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.baseCost = baseCost;
        this.costFactor = costFactor;
        this.maxLevel = maxLevel;
        this.requirements = requirements == null
                ? Collections.<Requirement>emptyList()
                : Collections.unmodifiableList(requirements);
    }

    public Cost costForNextLevel(int currentLevel) {
        return Formulas.levelCost(baseCost, costFactor, currentLevel + 1);
    }
}
