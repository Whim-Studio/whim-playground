package com.whim.cardwoven.engine;

import com.whim.cardwoven.api.Enums.CardType;
import com.whim.cardwoven.api.Enums.Faction;
import com.whim.cardwoven.api.Views.CardView;
import com.whim.cardwoven.domain.Card;
import com.whim.cardwoven.domain.CardLibrary;
import com.whim.cardwoven.domain.GameState;
import com.whim.cardwoven.domain.PlayerState;

// SinLogic keys off the domain's Card.isPowerful() flag (set on The Unfaithful's
// Raid/Warband/Plunder/Dark Pact) so the "power now, sin later" cards are defined
// by the domain, not guessed here.

/**
 * Encodes The Unfaithful's bargain: powerful ECONOMY / MILITARY cards are strong
 * now but seed SIN cards into the caster's OWN deck as the price. SIN cards are
 * un-playable dead weight (rejected by every engine action) that clog future
 * draws until discarded.
 *
 * Only The Unfaithful pays this cost; other factions never gain SIN.
 */
final class SinLogic {

    /** Truly potent cards (high attack) seed two sins instead of one. */
    private static final int VERY_POWERFUL_ATTACK = 6;

    private final GameState state;

    SinLogic(GameState state) {
        this.state = state;
    }

    /** True if this card is dead SIN weight and can never be played. */
    static boolean isSin(CardView card) {
        return card != null && card.type() == CardType.SIN;
    }

    /**
     * Applies the sin cost for {@code player} having just played {@code card}.
     * No-op unless the player is The Unfaithful and the card is flagged powerful
     * by the domain. Returns a log fragment describing sins added, or null.
     */
    String applySinCost(PlayerState player, Card card) {
        if (player.faction() != Faction.THE_UNFAITHFUL || card == null
                || !card.isPowerful()) {
            return null;
        }
        int sins = (card.type() == CardType.MILITARY
                && card.attack() >= VERY_POWERFUL_ATTACK) ? 2 : 1;
        for (int i = 0; i < sins; i++) {
            // Bury the sin in the deck so it surfaces on a later draw.
            player.deck().addBottom(CardLibrary.sinCard());
        }
        return "The Unfaithful's power breeds " + sins + " Sin card(s) into their deck";
    }
}
