package com.whim.nextrun.domain;

/**
 * A hostile entity. Carries enough state for all three paths of resolution:
 * combat (hp/attack/defense), economy (bribeCost), and stealth (sneakDc).
 */
public final class Enemy {
    public final String name;
    public int hp;
    public final int attack;
    public final int defense;
    public final int bribeCost;   // gold to buy passage
    public final int sneakDc;     // difficulty to slip past
    public final int tier;        // scales with wave number

    public Enemy(String name, int hp, int attack, int defense,
                 int bribeCost, int sneakDc, int tier) {
        this.name = name;
        this.hp = hp;
        this.attack = attack;
        this.defense = defense;
        this.bribeCost = bribeCost;
        this.sneakDc = sneakDc;
        this.tier = tier;
    }

    public boolean isDead() {
        return hp <= 0;
    }
}
