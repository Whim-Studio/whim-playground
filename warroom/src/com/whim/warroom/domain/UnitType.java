package com.whim.warroom.domain;

/**
 * Immutable archetype/template for a unit. Live {@link Unit}s reference a shared
 * {@code UnitType} for their static stats.
 *
 * <ul>
 *   <li>{@code speed} — world units per <b>second</b> at terrain mul 1.0.</li>
 *   <li>{@code range} — engagement range in world units.</li>
 *   <li>{@code attack}/{@code defense} — abstract combat points.</li>
 *   <li>{@code maxMorale} — 0..100.</li>
 * </ul>
 */
public final class UnitType {
    private final String id;
    private final String name;
    private final Era era;
    private final double maxHealth;
    private final double attack;
    private final double defense;
    private final double speed;
    private final double range;
    private final double maxMorale;

    public UnitType(String id, String name, Era era, double maxHealth, double attack,
                    double defense, double speed, double range, double maxMorale) {
        this.id = id;
        this.name = name;
        this.era = era;
        this.maxHealth = maxHealth;
        this.attack = attack;
        this.defense = defense;
        this.speed = speed;
        this.range = range;
        this.maxMorale = maxMorale;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Era getEra() {
        return era;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public double getAttack() {
        return attack;
    }

    public double getDefense() {
        return defense;
    }

    public double getSpeed() {
        return speed;
    }

    public double getRange() {
        return range;
    }

    public double getMaxMorale() {
        return maxMorale;
    }
}
