package com.dglz.domain;

import java.util.Collections;
import java.util.List;

/** Coach Mode value object: produced by Task 2, consumed by Task 3. */
public final class MoveSuggestion {
    private final Combination play;          // null == recommend PASS
    private final List<Card> highlightCards; // empty if pass
    private final String explanation;

    public MoveSuggestion(Combination play, List<Card> highlightCards, String explanation) {
        this.play = play;
        this.highlightCards = (highlightCards == null)
            ? Collections.<Card>emptyList()
            : Collections.unmodifiableList(highlightCards);
        this.explanation = explanation;
    }

    public Combination play() {
        return play;
    }

    public List<Card> highlightCards() {
        return highlightCards;
    }

    public String explanation() {
        return explanation;
    }
}
