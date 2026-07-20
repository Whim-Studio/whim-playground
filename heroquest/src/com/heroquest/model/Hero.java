package com.heroquest.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** A player-controlled Hero. Heroes defend on White Shields and roll 2d6 to move. */
public final class Hero extends Entity {
    private final HeroType type;
    private final List<Spell> spells = new ArrayList<Spell>();
    private int gold;

    public Hero(HeroType type) {
        super(type.getLabel(), type.getBody(), type.getMind(),
              type.getAttackDice(), type.getDefendDice());
        this.type = type;
    }

    public HeroType getType() {
        return type;
    }

    public List<Spell> getSpells() {
        return spells;
    }

    public void addSpell(Spell spell) {
        spells.add(spell);
    }

    public void removeSpell(Spell spell) {
        spells.remove(spell);
    }

    public boolean canCastSpells() {
        return type.canCastSpells();
    }

    public int getGold() {
        return gold;
    }

    public void addGold(int amount) {
        gold += amount;
    }

    @Override
    public CombatDie defendingShield() {
        return CombatDie.WHITE_SHIELD;
    }

    @Override
    public int baseMovement() {
        // Heroes roll 2d6 each turn; the roll is applied by the TurnManager.
        return 0;
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
