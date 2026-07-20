package com.heroquest.model;

import java.awt.Color;

/** A Zargon-controlled Monster. Monsters defend on Black Shields and have fixed movement. */
public final class Monster extends Entity {
    private final MonsterType type;

    public Monster(MonsterType type) {
        super(type.getLabel(), type.getBody(), type.getMind(),
              type.getAttackDice(), type.getDefendDice());
        this.type = type;
    }

    public MonsterType getType() {
        return type;
    }

    @Override
    public CombatDie defendingShield() {
        return CombatDie.BLACK_SHIELD;
    }

    @Override
    public int baseMovement() {
        return type.getMove();
    }

    @Override
    public Color getColor() {
        return type.getColor();
    }

    @Override
    public String getKindLabel() {
        return type.getLabel();
    }
}
