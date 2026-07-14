package com.whim.b5db.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * An immutable card definition loaded from the JSON card DSL. Card instances
 * are shared (flyweight): ownership/zone is tracked by the engine, not the card.
 */
public final class Card {

    private final String id;
    private final String name;
    private final Faction faction;
    private final CardType type;
    private final int cost;
    private final int prestige;
    private final Map<ContestType, Integer> attributes;
    private final ContestType contest;   // nullable; CONFLICT cards only
    private final int difficulty;        // CONFLICT cards only
    private final List<Effect> effects;
    private final String text;

    public Card(String id, String name, Faction faction, CardType type, int cost, int prestige,
                Map<ContestType, Integer> attributes, ContestType contest, int difficulty,
                List<Effect> effects, String text) {
        this.id = id;
        this.name = name;
        this.faction = faction;
        this.type = type;
        this.cost = cost;
        this.prestige = prestige;
        this.attributes = new EnumMap<>(ContestType.class);
        if (attributes != null) {
            this.attributes.putAll(attributes);
        }
        this.contest = contest;
        this.difficulty = difficulty;
        this.effects = effects == null ? new ArrayList<>() : new ArrayList<>(effects);
        this.text = text == null ? "" : text;
    }

    public String id() { return id; }
    public String name() { return name; }
    public Faction faction() { return faction; }
    public CardType type() { return type; }
    public int cost() { return cost; }
    public int prestige() { return prestige; }
    public ContestType contest() { return contest; }
    public int difficulty() { return difficulty; }
    public String text() { return text; }
    public boolean permanent() { return type.permanent(); }

    public List<Effect> effects() {
        return Collections.unmodifiableList(effects);
    }

    /** @return the card's rating in one attribute (0 if absent). */
    public int attribute(ContestType t) {
        Integer v = attributes.get(t);
        return v == null ? 0 : v;
    }

    /** @return sum of all four attribute ratings; a rough "power" heuristic. */
    public int totalAttributes() {
        int sum = 0;
        for (ContestType t : ContestType.values()) {
            sum += attribute(t);
        }
        return sum;
    }

    @Override
    public String toString() {
        return name + " [" + faction.display() + ", " + type + ", cost " + cost + "]";
    }
}
