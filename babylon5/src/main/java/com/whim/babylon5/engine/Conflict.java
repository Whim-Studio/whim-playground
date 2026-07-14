package com.whim.babylon5.engine;

import java.util.ArrayList;
import java.util.List;

import com.whim.babylon5.domain.Card;
import com.whim.babylon5.domain.ConflictType;

/**
 * A declared conflict plus the cards committed to it.
 *
 * <p>Per the rulebook ("The Conflict Round"), a conflict is declared by the initiator
 * naming a {@link ConflictType} and a target; characters and fleets are then rotated
 * during the Action round to either {@code support} the initiator's side or
 * {@code oppose} it. This object is the mutable record of those commitments; the actual
 * win/loss math lives in {@link GameEngine#resolveConflict(Conflict)} and is reported via
 * {@link ConflictResult}.
 */
public final class Conflict {

    private final int initiatorIndex;
    private final ConflictType type;
    private final int targetIndex;
    /**
     * The card that generated this conflict — a {@link com.whim.babylon5.domain.CardType#CONFLICT}
     * card played from hand, or the {@link com.whim.babylon5.domain.CardType#AGENDA} used to
     * initiate it. {@code null} only for synthetic/test conflicts.
     */
    private final Card sourceCard;
    private final List<Card> support = new ArrayList<Card>();
    private final List<Card> opposition = new ArrayList<Card>();

    public Conflict(int initiatorIndex, ConflictType type, int targetIndex) {
        this(initiatorIndex, type, targetIndex, null);
    }

    public Conflict(int initiatorIndex, ConflictType type, int targetIndex, Card sourceCard) {
        this.initiatorIndex = initiatorIndex;
        this.type = type;
        this.targetIndex = targetIndex;
        this.sourceCard = sourceCard;
    }

    /** The conflict card or agenda that initiated this conflict (may be {@code null}). */
    public Card getSourceCard() {
        return sourceCard;
    }

    public int getInitiator() {
        return initiatorIndex;
    }

    public ConflictType getType() {
        return type;
    }

    public int getTarget() {
        return targetIndex;
    }

    /** The initiator's committed supporters (mutable; cards are added during the Action round). */
    public List<Card> getSupport() {
        return support;
    }

    /** The target's committed opposers (mutable; cards are added during the Action round). */
    public List<Card> getOpposition() {
        return opposition;
    }
}
