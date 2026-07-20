package com.heroquest.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Factory + holders for the game's card decks: Treasure, elemental Spells and Artifacts.
 * Spell decks are built per element so a spellcasting Hero receives a full set.
 */
public final class Decks {

    private Decks() {
    }

    /** A drawable Treasure result. */
    public static final class TreasureCard {
        public enum Kind { GOLD, HEAL, WANDERING_MONSTER, NOTHING, HAZARD }

        private final String text;
        private final Kind kind;
        private final int value;

        public TreasureCard(String text, Kind kind, int value) {
            this.text = text;
            this.kind = kind;
            this.value = value;
        }

        public String getText() {
            return text;
        }

        public Kind getKind() {
            return kind;
        }

        public int getValue() {
            return value;
        }
    }

    /** A named, reusable magic item. */
    public static final class Artifact {
        private final String name;
        private final String effect;

        public Artifact(String name, String effect) {
            this.name = name;
            this.effect = effect;
        }

        public String getName() {
            return name;
        }

        public String getEffect() {
            return effect;
        }
    }

    public static Deque<TreasureCard> buildTreasureDeck(Random rng) {
        List<TreasureCard> cards = new ArrayList<TreasureCard>();
        for (int i = 0; i < 8; i++) {
            cards.add(new TreasureCard("You find " + (i + 1) * 25 + " gold coins.",
                    TreasureCard.Kind.GOLD, (i + 1) * 25));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new TreasureCard("A healing potion restores 2 Body Points.",
                    TreasureCard.Kind.HEAL, 2));
        }
        for (int i = 0; i < 4; i++) {
            cards.add(new TreasureCard("Wandering Monster! A monster appears and attacks.",
                    TreasureCard.Kind.WANDERING_MONSTER, 0));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(new TreasureCard("A hidden dart springs out. Lose 1 Body Point.",
                    TreasureCard.Kind.HAZARD, 1));
        }
        for (int i = 0; i < 6; i++) {
            cards.add(new TreasureCard("You find nothing of value.",
                    TreasureCard.Kind.NOTHING, 0));
        }
        Collections.shuffle(cards, rng);
        return new ArrayDeque<TreasureCard>(cards);
    }

    /** The 12 base spells: three per element. */
    public static List<Spell> buildSpellSet(SpellElement element) {
        List<Spell> spells = new ArrayList<Spell>();
        switch (element) {
            case FIRE:
                spells.add(new Spell("Ball of Flame", element, Spell.Effect.DAMAGE, 2));
                spells.add(new Spell("Courage", element, Spell.Effect.DEFEND, 2));
                spells.add(new Spell("Fire of Wrath", element, Spell.Effect.DAMAGE, 1));
                break;
            case EARTH:
                spells.add(new Spell("Heal Body", element, Spell.Effect.HEAL, 4));
                spells.add(new Spell("Rock Skin", element, Spell.Effect.DEFEND, 2));
                spells.add(new Spell("Pass Through Rock", element, Spell.Effect.PASS, 0));
                break;
            case WATER:
                spells.add(new Spell("Sleep", element, Spell.Effect.PASS, 0));
                spells.add(new Spell("Water of Healing", element, Spell.Effect.HEAL, 4));
                spells.add(new Spell("Veil of Mist", element, Spell.Effect.DEFEND, 2));
                break;
            case AIR:
                spells.add(new Spell("Genie", element, Spell.Effect.DAMAGE, 3));
                spells.add(new Spell("Swift Wind", element, Spell.Effect.PASS, 0));
                spells.add(new Spell("Tempest", element, Spell.Effect.PASS, 0));
                break;
            default:
                break;
        }
        return spells;
    }

    public static List<Artifact> buildArtifacts() {
        List<Artifact> items = new ArrayList<Artifact>();
        items.add(new Artifact("Spirit Blade", "Adds 1 Attack die against undead."));
        items.add(new Artifact("Talisman of Lore", "Allows an extra spell to be cast per quest."));
        items.add(new Artifact("Wand of Magic", "Adds 1 Attack die to any spell that inflicts damage."));
        items.add(new Artifact("Borin's Armour", "Adds 2 Defend dice but reduces movement by 1."));
        return items;
    }
}
