package com.whim.nextrun.domain;

/**
 * The hero. Tracks live stats (which grow during a run), resources, simple
 * equipment, and position. The engine reads these to compute every turn cost
 * and resolution outcome.
 */
public final class Player {
    public final HeroClass heroClass;
    public Position pos;

    public int maxHp;
    public int hp;
    public int attack;
    public int defense;
    public int dexterity;
    public int magic;

    public int gold;
    public int materials;

    public int weaponBonus = 0; // crafted gear adds to attack
    public int armorBonus = 0;  // crafted gear adds to defense
    public int structuresBuilt = 0;

    public Player(HeroClass heroClass, Position start) {
        this.heroClass = heroClass;
        this.pos = start;
        this.maxHp = heroClass.baseHp();
        this.hp = heroClass.baseHp();
        this.attack = heroClass.baseAttack();
        this.defense = heroClass.baseDefense();
        this.dexterity = heroClass.baseDexterity();
        this.magic = heroClass.baseMagic();
        this.gold = heroClass.baseGold();
        this.materials = 2;
    }

    public int effectiveAttack() { return attack + weaponBonus; }
    public int effectiveDefense() { return defense + armorBonus; }

    public boolean isDead() { return hp <= 0; }

    public void heal(int amount) {
        hp = Math.min(maxHp, hp + amount);
    }

    public void damage(int amount) {
        hp = Math.max(0, hp - amount);
    }
}
