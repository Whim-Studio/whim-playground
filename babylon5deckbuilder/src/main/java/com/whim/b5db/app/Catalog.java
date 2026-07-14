package com.whim.b5db.app;

import com.whim.b5db.engine.BasicCards;
import com.whim.b5db.io.CardLoader;
import com.whim.b5db.model.Card;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and indexes the full card catalogue at startup from two sources:
 * bundled classpath resources ({@code /cards/*.json}) and an optional external
 * {@code assets/cards} directory (so dropping in a new JSON file and restarting
 * makes the card available). Basic/starter/hero cards are always included in
 * the id index for save/load resolution.
 */
public final class Catalog {

    private final List<Card> cards;
    private final Map<String, Card> index;

    private Catalog(List<Card> cards, Map<String, Card> index) {
        this.cards = cards;
        this.index = index;
    }

    public List<Card> cards() {
        return cards;
    }

    public Map<String, Card> index() {
        return index;
    }

    /** Load bundled cards plus any in {@code externalDir} (may be null/missing). */
    public static Catalog load(File externalDir) {
        CardLoader loader = new CardLoader();
        List<Card> cards = new ArrayList<>(loader.loadClasspathIndex());
        if (externalDir != null && externalDir.isDirectory()) {
            cards.addAll(loader.loadDirectory(externalDir));
        }

        Map<String, Card> index = new LinkedHashMap<>();
        for (Card c : BasicCards.allBasics()) {
            index.put(c.id(), c);
        }
        for (Card c : cards) {
            index.put(c.id(), c);
        }
        return new Catalog(cards, index);
    }
}
