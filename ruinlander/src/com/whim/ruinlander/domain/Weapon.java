package com.whim.ruinlander.domain;

/** A weapon item. Carries combat stats consumed by the engine. */
public class Weapon extends Item {
    private final WeaponClass weaponClass;
    private final int damage;
    private final double accuracy;   // base hit chance 0..1
    private final int apCost;        // action points to attack
    private final int range;         // tiles (1 = melee)
    private final String ammoItemId; // null/empty for melee or unlimited

    public Weapon(String id, String name, double weight,
                  WeaponClass weaponClass, int damage, double accuracy,
                  int apCost, int range, String ammoItemId) {
        super(id, name, ItemCategory.WEAPON, weight, false);
        this.weaponClass = weaponClass;
        this.damage = damage;
        this.accuracy = accuracy;
        this.apCost = apCost;
        this.range = range;
        this.ammoItemId = ammoItemId;
    }

    public WeaponClass getWeaponClass() { return weaponClass; }
    public int getDamage() { return damage; }
    public double getAccuracy() { return accuracy; }
    public int getApCost() { return apCost; }
    public int getRange() { return range; }
    public String getAmmoItemId() { return ammoItemId; }

    public boolean usesAmmo() {
        return ammoItemId != null && !ammoItemId.isEmpty();
    }
}
