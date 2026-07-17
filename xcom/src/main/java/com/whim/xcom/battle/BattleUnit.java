package com.whim.xcom.battle;

import com.whim.xcom.rules.def.ArmorDef;
import com.whim.xcom.rules.def.WeaponDef;

/**
 * A single combatant on the battlescape (soldier or alien). Mutable state that
 * the engine advances; all formulas live in the ruleset, this only holds numbers.
 *
 * <p>Facing is an octant 0..7 (0 = North, 2 = East, 4 = South, 6 = West).</p>
 */
public final class BattleUnit {

    private final String id;
    private final String name;
    private final Side side;

    private int x;
    private int y;
    private int facing = 4; // soldiers start facing "up"/north-ish; set at deploy

    private final int maxTU;
    private int tu;

    private final int maxHealth;
    private int health;

    private int morale = 100;

    private final int firingAccuracy;
    private final int reactions;
    private final int strength;

    private final WeaponDef weapon;
    private final ArmorDef armor;
    private int ammo;

    private boolean kneeling;
    private boolean alive = true;
    private boolean reactionSpent; // has this unit already reacted since it last acted
    private int grenades;

    public BattleUnit(String id, String name, Side side,
                      int maxTU, int maxHealth, int firingAccuracy, int reactions, int strength,
                      WeaponDef weapon, ArmorDef armor) {
        this.id = id;
        this.name = name;
        this.side = side;
        this.maxTU = maxTU;
        this.tu = maxTU;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.firingAccuracy = firingAccuracy;
        this.reactions = reactions;
        this.strength = strength;
        this.weapon = weapon;
        this.armor = armor;
        this.ammo = (weapon != null && weapon.clipSize() > 0) ? weapon.clipSize() : Integer.MAX_VALUE;
    }

    public String id() { return id; }
    public String name() { return name; }
    public Side side() { return side; }
    public boolean alien() { return side == Side.ALIEN; }

    public int x() { return x; }
    public int y() { return y; }
    public void setPos(int x, int y) { this.x = x; this.y = y; }

    public int facing() { return facing; }
    public void setFacing(int facing) { this.facing = ((facing % 8) + 8) % 8; }

    public int maxTU() { return maxTU; }
    public int tu() { return tu; }
    public void spendTU(int amount) { tu = Math.max(0, tu - amount); }
    public void refreshTU() { tu = maxTU; reactionSpent = false; }
    public boolean hasTU(int amount) { return tu >= amount; }

    public int maxHealth() { return maxHealth; }
    public int health() { return health; }

    public int morale() { return morale; }
    public void changeMorale(int delta) { morale = Math.max(0, Math.min(100, morale + delta)); }

    public int firingAccuracy() { return firingAccuracy; }
    public int reactions() { return reactions; }
    public int strength() { return strength; }

    public WeaponDef weapon() { return weapon; }
    public ArmorDef armor() { return armor; }
    public int ammo() { return ammo; }
    public boolean consumeAmmo() {
        if (ammo <= 0) {
            return false;
        }
        if (ammo != Integer.MAX_VALUE) {
            ammo--;
        }
        return true;
    }

    public boolean kneeling() { return kneeling; }
    public void setKneeling(boolean kneeling) { this.kneeling = kneeling; }

    public boolean alive() { return alive; }

    public boolean reactionSpent() { return reactionSpent; }
    public void markReacted() { reactionSpent = true; }

    public int grenades() { return grenades; }
    public void setGrenades(int n) { this.grenades = Math.max(0, n); }
    public boolean useGrenade() {
        if (grenades <= 0) {
            return false;
        }
        grenades--;
        return true;
    }

    /** Apply health damage; returns true if this blow killed the unit. */
    public boolean applyDamage(int amount) {
        if (amount <= 0 || !alive) {
            return false;
        }
        health -= amount;
        if (health <= 0) {
            health = 0;
            alive = false;
            return true;
        }
        return false;
    }
}
