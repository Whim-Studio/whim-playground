package com.whim.oggalaxy.api;

import java.io.Serializable;

/**
 * A single prerequisite: either a building at some level or a technology at some
 * level. Buildings/ships/techs list the requirements that must be met before they
 * can be constructed or researched.
 */
public final class Requirement implements Serializable {

    private static final long serialVersionUID = 1L;

    public final Ids.BuildingType building; // null if this is a tech requirement
    public final Ids.TechType tech;         // null if this is a building requirement
    public final int level;

    private Requirement(Ids.BuildingType building, Ids.TechType tech, int level) {
        this.building = building;
        this.tech = tech;
        this.level = level;
    }

    public static Requirement of(Ids.BuildingType b, int level) {
        return new Requirement(b, null, level);
    }

    public static Requirement of(Ids.TechType t, int level) {
        return new Requirement(null, t, level);
    }

    public boolean isBuilding() {
        return building != null;
    }

    public String label() {
        return (isBuilding() ? building.name() : tech.name()) + " " + level;
    }
}
