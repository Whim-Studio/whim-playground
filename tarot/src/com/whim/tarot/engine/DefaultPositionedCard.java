package com.whim.tarot.engine;

import com.whim.tarot.domain.DrawnCard;
import com.whim.tarot.domain.SpreadPosition;

/**
 * Immutable {@link PositionedCard} value pairing one spread position with the
 * card drawn into it.
 */
public final class DefaultPositionedCard implements PositionedCard {
    private final SpreadPosition position;
    private final DrawnCard drawnCard;

    public DefaultPositionedCard(SpreadPosition position, DrawnCard drawnCard) {
        if (position == null) {
            throw new IllegalArgumentException("position must not be null");
        }
        if (drawnCard == null) {
            throw new IllegalArgumentException("drawnCard must not be null");
        }
        this.position = position;
        this.drawnCard = drawnCard;
    }

    @Override
    public SpreadPosition getPosition() {
        return position;
    }

    @Override
    public DrawnCard getDrawnCard() {
        return drawnCard;
    }

    @Override
    public String toString() {
        return position.getName() + ": " + drawnCard.getCard().getName()
                + " (" + drawnCard.getOrientation() + ")";
    }
}
