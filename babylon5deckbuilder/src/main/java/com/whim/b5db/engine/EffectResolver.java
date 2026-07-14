package com.whim.b5db.engine;

import com.whim.b5db.model.Card;
import com.whim.b5db.model.ContestType;
import com.whim.b5db.model.Effect;

import java.util.List;

/**
 * Interprets the JSON card-effect DSL against live game state. Each verb in
 * {@link Effect.Type} is handled here; adding a new verb means adding a case
 * (and a DSL name) without touching card data.
 */
public final class EffectResolver {

    private EffectResolver() {
    }

    /** Apply every effect on a card as it is played or triggered. */
    public static void applyAll(Card card, GameState state, PlayerState owner) {
        for (Effect e : card.effects()) {
            apply(e, state, owner);
        }
    }

    public static void apply(Effect e, GameState state, PlayerState owner) {
        switch (e.type()) {
            case GAIN_INFLUENCE:
                owner.addInfluence(e.amount());
                break;
            case GAIN_PRESTIGE:
            case ADD_AGENDA_PROGRESS: // progress banks directly as PRESTIGE in this model
                owner.addPrestige(e.amount());
                break;
            case GAIN_ATTRIBUTE:
                if (e.attribute() != null) {
                    owner.addPool(e.attribute(), e.amount());
                }
                break;
            case DRAW:
                owner.draw(e.amount(), state.rng());
                break;
            case TRASH:
                trashWeakest(owner, e.amount());
                break;
            case DISCARD_OPPONENT:
                discardOpponents(state, owner, e.amount());
                break;
            case CONVERT_DIPLOMACY:
                convertDiplomacy(owner);
                break;
            default:
                break;
        }
    }

    /** Move DIPLOMACY pool 1:1 into INFLUENCE (the only attribute→currency link). */
    public static void convertDiplomacy(PlayerState owner) {
        int dip = owner.pool(ContestType.DIPLOMACY);
        if (dip > 0) {
            owner.spendPool(ContestType.DIPLOMACY, dip);
            owner.addInfluence(dip);
        }
    }

    private static void trashWeakest(PlayerState owner, int n) {
        List<Card> hand = owner.hand();
        for (int k = 0; k < n && !hand.isEmpty(); k++) {
            int weakest = 0;
            for (int j = 1; j < hand.size(); j++) {
                if (rank(hand.get(j)) < rank(hand.get(weakest))) {
                    weakest = j;
                }
            }
            owner.outOfGame().add(hand.remove(weakest));
        }
    }

    private static int rank(Card c) {
        // Prefer trashing low-value economy first.
        return c.cost() * 10 + c.totalAttributes();
    }

    private static void discardOpponents(GameState state, PlayerState owner, int n) {
        for (PlayerState p : state.players()) {
            if (p == owner) {
                continue;
            }
            for (int k = 0; k < n && !p.hand().isEmpty(); k++) {
                p.discard().add(p.hand().remove(p.hand().size() - 1));
            }
        }
    }
}
