package com.whim.scg.model;

import com.whim.scg.api.Enums;
import com.whim.scg.api.GridPos;
import com.whim.scg.api.Views;

/** Mutable crew member (player or hostile). */
public final class CrewModel implements Views.CrewView {
    public int id;
    public String name;
    public Enums.Faction faction;
    public Enums.CrewRole role;
    public int hp;
    public int maxHp;
    public int happiness = 75;
    public int level = 1;
    public int xp;
    public final int[] skills = new int[Enums.StatType.values().length];
    public int stationRoomId = -1;
    public GridPos boardingPos; // null unless boarding
    /** Cooldown accumulator used by hostile boarding AI. */
    public double actTimer;
    /** Fractional damage / heal accumulators for per-tick sub-integer effects. */
    public double dmgAccum, healAccum;

    /** Apply fractional damage, spending whole points as they accrue. */
    public void hurt(double amount) {
        dmgAccum += amount;
        while (dmgAccum >= 1.0 && hp > 0) { hp--; dmgAccum -= 1.0; }
        if (hp <= 0) { hp = 0; dmgAccum = 0; }
    }

    /** Apply fractional healing, spending whole points as they accrue. */
    public void heal(double amount) {
        if (hp <= 0) return;
        healAccum += amount;
        while (healAccum >= 1.0 && hp < maxHp) { hp++; healAccum -= 1.0; }
        if (hp >= maxHp) healAccum = 0;
    }

    public CrewModel() {}

    public void setSkill(Enums.StatType s, int v) { skills[s.ordinal()] = clamp(v); }
    public void addSkill(Enums.StatType s, int v) { skills[s.ordinal()] = clamp(skills[s.ordinal()] + v); }

    private static int clamp(int v) { return v < 0 ? 0 : (v > 100 ? 100 : v); }

    public void gainXp(int amount) {
        xp += amount;
        while (xp >= level * 100) {
            xp -= level * 100;
            level++;
            maxHp += 2;
            hp = Math.min(maxHp, hp + 2);
            addSkill(role.primary(), 4);
        }
    }

    @Override public int id() { return id; }
    @Override public String name() { return name; }
    @Override public Enums.Faction faction() { return faction; }
    @Override public Enums.CrewRole role() { return role; }
    @Override public int hp() { return hp; }
    @Override public int maxHp() { return maxHp; }
    @Override public int happiness() { return happiness; }
    @Override public int level() { return level; }
    @Override public int xp() { return xp; }
    @Override public int skill(Enums.StatType s) { return skills[s.ordinal()]; }
    @Override public int stationRoomId() { return stationRoomId; }
    @Override public GridPos boardingPos() { return boardingPos; }
    @Override public boolean alive() { return hp > 0; }
}
