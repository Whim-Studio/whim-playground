package com.whim.firetop.model;

import java.io.Serializable;

/**
 * A single card from one of the three decks. Carries original flavor text and a
 * mechanical {@link CardEffect} the engine resolves. For {@code ENCOUNTER_MONSTER}
 * cards, an attached {@link Monster} template describes the foe.
 */
public final class Card implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final CardType type;
    private final String description;
    private final CardEffect effect;
    private final int magnitude;
    private final Monster monster; // non-null only for ENCOUNTER_MONSTER

    public Card(String name, CardType type, String description, CardEffect effect, int magnitude) {
        this(name, type, description, effect, magnitude, null);
    }

    public Card(String name, CardType type, String description, CardEffect effect, int magnitude, Monster monster) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.effect = effect;
        this.magnitude = magnitude;
        this.monster = monster;
    }

    public String getName() { return name; }
    public CardType getType() { return type; }
    public String getDescription() { return description; }
    public CardEffect getEffect() { return effect; }
    public int getMagnitude() { return magnitude; }
    public Monster getMonster() { return monster == null ? null : monster.copy(); }

    @Override
    public String toString() { return name; }
}
