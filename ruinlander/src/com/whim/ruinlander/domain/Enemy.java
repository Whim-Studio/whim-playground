package com.whim.ruinlander.domain;

import java.awt.Color;

/** A hostile combatant entity. Pure data; the engine drives its behaviour. */
public class Enemy implements Entity {
    private final EntityType type;
    private final String name;
    private int hp;
    private final int maxHp;
    private int ap;
    private final int attack;       // base damage
    private final double accuracy;  // base hit chance 0..1
    private final double armorReduction; // 0..1 incoming damage absorbed
    private final Faction faction;
    private Position position;

    public Enemy(EntityType type, String name, int maxHp, int ap,
                 int attack, double accuracy, double armorReduction,
                 Faction faction, Position position) {
        this.type = type;
        this.name = name;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.ap = ap;
        this.attack = attack;
        this.accuracy = accuracy;
        this.armorReduction = armorReduction;
        this.faction = faction;
        this.position = position;
    }

    public String getName() { return name; }
    public int getHp() { return hp; }
    public void setHp(int hp) { this.hp = Math.max(0, Math.min(hp, maxHp)); }
    public int getMaxHp() { return maxHp; }
    public int getAp() { return ap; }
    public void setAp(int ap) { this.ap = Math.max(0, ap); }
    public int getAttack() { return attack; }
    public double getAccuracy() { return accuracy; }
    public double getArmorReduction() { return armorReduction; }
    public Faction getFaction() { return faction; }

    public boolean isDead() { return hp <= 0; }

    @Override
    public EntityType getType() { return type; }

    @Override
    public Position getPosition() { return position; }

    @Override
    public void setPosition(Position p) { this.position = p; }

    @Override
    public String glyph() {
        switch (type) {
            case RAIDER: return "R";
            case MUTANT: return "M";
            case ANIMAL: return "a";
            default: return "E";
        }
    }

    @Override
    public Color color() {
        switch (type) {
            case RAIDER: return new Color(200, 60, 60);
            case MUTANT: return new Color(120, 200, 70);
            case ANIMAL: return new Color(180, 140, 90);
            default: return Color.RED;
        }
    }
}
