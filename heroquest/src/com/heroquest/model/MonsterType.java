package com.heroquest.model;

import java.awt.Color;

/**
 * Zargon's monster roster with canonical HeroQuest stats.
 * move is the fixed number of squares a monster travels (monsters do not roll movement).
 */
public enum MonsterType {
    GOBLIN("Goblin", 'g', 1, 0, 2, 1, 10, new Color(0x6B, 0x8E, 0x23)),
    ORC("Orc", 'o', 1, 0, 3, 2, 8, new Color(0x55, 0x6B, 0x2F)),
    FIMIR("Fimir", 'f', 2, 0, 3, 3, 6, new Color(0x8B, 0x5A, 0x2B)),
    SKELETON("Skeleton", 's', 1, 0, 2, 2, 6, new Color(0xE0, 0xE0, 0xD0)),
    ZOMBIE("Zombie", 'z', 1, 0, 2, 3, 4, new Color(0x8A, 0x9A, 0x5B)),
    MUMMY("Mummy", 'm', 2, 0, 3, 4, 4, new Color(0xC9, 0xB8, 0x8B)),
    CHAOS_WARRIOR("Chaos Warrior", 'c', 3, 0, 4, 5, 6, new Color(0x7A, 0x1F, 0x2B)),
    GARGOYLE("Gargoyle", 'G', 3, 0, 4, 5, 6, new Color(0x50, 0x50, 0x58));

    private final String label;
    private final char glyph;
    private final int body;
    private final int mind;
    private final int attackDice;
    private final int defendDice;
    private final int move;
    private final Color color;

    MonsterType(String label, char glyph, int body, int mind,
                int attackDice, int defendDice, int move, Color color) {
        this.label = label;
        this.glyph = glyph;
        this.body = body;
        this.mind = mind;
        this.attackDice = attackDice;
        this.defendDice = defendDice;
        this.move = move;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public char getGlyph() {
        return glyph;
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

    public int getMove() {
        return move;
    }

    public Color getColor() {
        return color;
    }
}
