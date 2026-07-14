package com.whim.necromunda.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * An immutable weapon profile. All behaviour is data: the combat rules read the
 * numeric profile and the {@link WeaponRule} flag set — no per-weapon subclasses.
 *
 * <p>Ranges are in tiles/inches. {@code saveMod} is armour penetration expressed
 * as a positive number that <em>worsens</em> the target's save (a save of 5+ with
 * a saveMod of 1 becomes 6+). {@code ammoRoll} is the minimum D6 needed to avoid
 * running dry when an ammo check is triggered (0 = never checks).
 */
public final class Weapon {

    private final String id;
    private final String name;
    private final int rangeShort;
    private final int rangeLong;
    private final int strength;
    private final int damage;
    private final int saveMod;
    private final int ammoRoll;
    private final int cost;
    private final Set<WeaponRule> rules;

    public Weapon(String id, String name, int rangeShort, int rangeLong, int strength,
                  int damage, int saveMod, int ammoRoll, int cost, Set<WeaponRule> rules) {
        this.id = id;
        this.name = name;
        this.rangeShort = rangeShort;
        this.rangeLong = rangeLong;
        this.strength = strength;
        this.damage = damage;
        this.saveMod = saveMod;
        this.ammoRoll = ammoRoll;
        this.cost = cost;
        this.rules = rules == null
                ? EnumSet.noneOf(WeaponRule.class)
                : EnumSet.copyOf(rules);
    }

    public String id() { return id; }
    public String name() { return name; }
    public int rangeShort() { return rangeShort; }
    public int rangeLong() { return rangeLong; }
    public int strength() { return strength; }
    public int damage() { return damage; }
    public int saveMod() { return saveMod; }
    public int ammoRoll() { return ammoRoll; }
    public int cost() { return cost; }

    public Set<WeaponRule> rules() {
        return Collections.unmodifiableSet(rules);
    }

    public boolean has(WeaponRule rule) {
        return rules.contains(rule);
    }

    /** True if this weapon can strike in the hand-to-hand phase. */
    public boolean usableInMelee() {
        return has(WeaponRule.MELEE) || has(WeaponRule.PISTOL);
    }

    /** True if this weapon has a ranged profile (can be fired in the shooting phase). */
    public boolean usableAtRange() {
        return !has(WeaponRule.MELEE);
    }

    @Override
    public String toString() {
        return name;
    }
}
