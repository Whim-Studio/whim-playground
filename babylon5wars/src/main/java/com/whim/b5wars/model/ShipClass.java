package com.whim.b5wars.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Immutable printed template a {@link Ship} is instantiated from. */
public final class ShipClass {
    private final String id;
    private final String name;
    private final Race race;
    private final int points;
    private final int maxSpeed;
    private final int turnMode;
    private final int thrust;
    private final int power;
    private final int initiativeBonus;
    private final int crewQuality;
    private final int sensorRating;
    private final int ewRating;
    private final Map<Facing, Integer> armor;
    private final Map<Section, Integer> structure;
    private final DefenseType defenseType;
    private final List<Weapon> weapons;
    private final List<Special> specials;

    public ShipClass(String id, String name, Race race, int points,
                     int maxSpeed, int turnMode, int thrust, int power,
                     int initiativeBonus, int crewQuality,
                     int sensorRating, int ewRating,
                     Map<Facing, Integer> armor,
                     Map<Section, Integer> structure,
                     DefenseType defenseType,
                     List<Weapon> weapons,
                     List<Special> specials) {
        this.id = id;
        this.name = name;
        this.race = race;
        this.points = points;
        this.maxSpeed = maxSpeed;
        this.turnMode = turnMode;
        this.thrust = thrust;
        this.power = power;
        this.initiativeBonus = initiativeBonus;
        this.crewQuality = crewQuality;
        this.sensorRating = sensorRating;
        this.ewRating = ewRating;
        Map<Facing, Integer> a = new EnumMap<Facing, Integer>(Facing.class);
        if (armor != null) {
            a.putAll(armor);
        }
        this.armor = Collections.unmodifiableMap(a);
        Map<Section, Integer> s = new EnumMap<Section, Integer>(Section.class);
        if (structure != null) {
            s.putAll(structure);
        }
        this.structure = Collections.unmodifiableMap(s);
        this.defenseType = defenseType;
        this.weapons = Collections.unmodifiableList(
                new ArrayList<Weapon>(weapons == null ? new ArrayList<Weapon>() : weapons));
        this.specials = Collections.unmodifiableList(
                new ArrayList<Special>(specials == null ? new ArrayList<Special>() : specials));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Race getRace() {
        return race;
    }

    public int getPoints() {
        return points;
    }

    public int getMaxSpeed() {
        return maxSpeed;
    }

    public int getTurnMode() {
        return turnMode;
    }

    public int getThrust() {
        return thrust;
    }

    public int getPower() {
        return power;
    }

    public int getInitiativeBonus() {
        return initiativeBonus;
    }

    public int getCrewQuality() {
        return crewQuality;
    }

    public int getSensorRating() {
        return sensorRating;
    }

    public int getEwRating() {
        return ewRating;
    }

    /** Per-facing armor values. */
    public Map<Facing, Integer> getArmor() {
        return armor;
    }

    /** Per-section structure (max boxes). */
    public Map<Section, Integer> getStructure() {
        return structure;
    }

    public DefenseType getDefenseType() {
        return defenseType;
    }

    public List<Weapon> getWeapons() {
        return weapons;
    }

    public List<Special> getSpecials() {
        return specials;
    }
}
