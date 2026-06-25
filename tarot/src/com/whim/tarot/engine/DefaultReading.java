package com.whim.tarot.engine;

import com.whim.tarot.domain.SpreadType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable {@link Reading} implementation.
 *
 * <p>The synthesis is supplied after construction via {@link #withSynthesis}
 * so the {@link ReadingInterpreter} can interpret a reading whose positioned
 * cards are already populated, then return a fully-populated copy.
 */
public final class DefaultReading implements Reading {
    private final SpreadType spreadType;
    private final List<PositionedCard> positionedCards;
    private final String synthesis;

    public DefaultReading(SpreadType spreadType, List<PositionedCard> positionedCards) {
        this(spreadType, positionedCards, "");
    }

    public DefaultReading(SpreadType spreadType, List<PositionedCard> positionedCards, String synthesis) {
        if (spreadType == null) {
            throw new IllegalArgumentException("spreadType must not be null");
        }
        if (positionedCards == null) {
            throw new IllegalArgumentException("positionedCards must not be null");
        }
        this.spreadType = spreadType;
        this.positionedCards = Collections.unmodifiableList(
                new ArrayList<PositionedCard>(positionedCards));
        this.synthesis = synthesis == null ? "" : synthesis;
    }

    /** Returns a copy of this reading with the given synthesis text. */
    public DefaultReading withSynthesis(String newSynthesis) {
        return new DefaultReading(spreadType, positionedCards, newSynthesis);
    }

    @Override
    public SpreadType getSpreadType() {
        return spreadType;
    }

    @Override
    public List<PositionedCard> getPositionedCards() {
        return positionedCards;
    }

    @Override
    public String getSynthesis() {
        return synthesis;
    }
}
