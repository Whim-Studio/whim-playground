package com.tiwas.rpg.engine;

import java.util.ArrayList;
import java.util.List;

import com.tiwas.rpg.domain.AttributeCode;
import com.tiwas.rpg.domain.Character;
import com.tiwas.rpg.domain.Skill;

/**
 * Rolls a fresh {@link Character}: 1d100 per attribute, one Tier-1 skill per
 * attribute, generalXP = 0, then restored to full pools.
 */
public final class CharacterGenerator {
    private final Dice dice;

    public CharacterGenerator(Dice dice) {
        if (dice == null) {
            throw new IllegalArgumentException("dice must not be null");
        }
        this.dice = dice;
    }

    public Character generate(String name) {
        Character c = new Character(name);

        for (AttributeCode a : AttributeCode.values()) {
            int rolled = dice.d100();
            c.setAttribute(a, rolled);

            List<String> formula = new ArrayList<String>();
            formula.add(a.code());
            // Tier-1 skill: name = tier1Skill, tier = 1, value = maxCap/2 = attrValue/2.
            Skill skill = new Skill(a.tier1Skill(), 1, formula, rolled / 2);
            c.putSkill(skill);
        }

        c.setGeneralXP(0);
        c.restoreToFull();
        return c;
    }
}
