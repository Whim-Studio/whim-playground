package com.whim.tarot.engine;

import com.whim.tarot.domain.SpreadType;

import java.util.List;

/**
 * The result of dealing a spread: the spread type, the cards mapped to their
 * positions (ordered by position index), and the synthesized plain-English
 * interpretation produced by {@link ReadingInterpreter}.
 */
public interface Reading {
    SpreadType getSpreadType();

    /** Positioned cards, size == spread card count, ordered by position index. */
    List<PositionedCard> getPositionedCards();

    /** Full plain-English synthesized interpretation. */
    String getSynthesis();
}
