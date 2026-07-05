package com.whim.oggalaxy.api;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Immutable definition of a planetary defense unit. Same combat model as ships but
 * defenses never move and (mostly) auto-rebuild after a successful defence.
 */
public final class DefenseDef implements Serializable {

    private static final long serialVersionUID = 1L;

    public final Ids.DefenseType type;
    public final String name;
    public final String description;

    public final double weapon;
    public final double shield;
    public final double hull;

    public final Cost cost;
    public final int maxCount;   // 0 == unlimited; shield domes are limited to 1
    public final boolean isShieldDome;

    public final List<Requirement> requirements;

    public DefenseDef(Ids.DefenseType type, String name, String description,
                      double weapon, double shield, double hull,
                      Cost cost, int maxCount, boolean isShieldDome, List<Requirement> requirements) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.weapon = weapon;
        this.shield = shield;
        this.hull = hull;
        this.cost = cost;
        this.maxCount = maxCount;
        this.isShieldDome = isShieldDome;
        this.requirements = requirements == null
                ? Collections.<Requirement>emptyList()
                : Collections.unmodifiableList(requirements);
    }
}
