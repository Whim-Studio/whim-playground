package com.heroquest.model;

import java.awt.Color;

/**
 * The four Heroes with their canonical HeroQuest starting stats.
 * body/mind are the starting point pools; attack/defend are dice counts.
 */
public enum HeroType {
    BARBARIAN("Barbarian", 8, 2, 3, 2, new Color(0xB5, 0x3A, 0x2E)),
    DWARF("Dwarf", 7, 3, 2, 2, new Color(0xC8, 0x8A, 0x2E)),
    ELF("Elf", 6, 4, 2, 2, new Color(0x2E, 0x8B, 0x57)),
    WIZARD("Wizard", 4, 6, 1, 2, new Color(0x4B, 0x4F, 0xC8));

    private final String label;
    private final int body;
    private final int mind;
    private final int attackDice;
    private final int defendDice;
    private final Color color;

    HeroType(String label, int body, int mind, int attackDice, int defendDice, Color color) {
        this.label = label;
        this.body = body;
        this.mind = mind;
        this.attackDice = attackDice;
        this.defendDice = defendDice;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public int getBody() {
        return body;
    }

    public int getMind() {
        return mind;
    }

    public int getAttackDice() {
        return attackDice;
    }

    public int getDefendDice() {
        return defendDice;
    }

    public Color getColor() {
        return color;
    }

    /** Only the Wizard and Elf may cast spells in the base game. */
    public boolean canCastSpells() {
        return this == WIZARD || this == ELF;
    }
}
