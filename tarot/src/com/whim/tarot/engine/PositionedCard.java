package com.whim.tarot.engine;

import com.whim.tarot.domain.DrawnCard;
import com.whim.tarot.domain.SpreadPosition;

/**
 * Pairs a {@link SpreadPosition} in a spread with the {@link DrawnCard} that
 * landed in it. The position supplies the slot's name and meaning; the drawn
 * card supplies the card and its orientation.
 */
public interface PositionedCard {
    SpreadPosition getPosition();
    DrawnCard getDrawnCard();
}
