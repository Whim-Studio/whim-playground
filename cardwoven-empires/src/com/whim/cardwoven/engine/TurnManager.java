package com.whim.cardwoven.engine;

import java.util.ArrayList;
import java.util.List;

import com.whim.cardwoven.api.Enums.GamePhase;
import com.whim.cardwoven.domain.Card;
import com.whim.cardwoven.domain.GameState;
import com.whim.cardwoven.domain.PlayerState;

/**
 * Sequences the phases of a single player's turn — DRAW, MAIN, COMBAT, YIELD,
 * DISCARD, END — and enforces the hand limit. This is a deckbuilder: at DRAW a
 * player fills a fresh hand up to their limit (base hand size + Idol bonus
 * draws), and at DISCARD any cards not played are dumped to the discard pile,
 * so nothing carries over. That cycling is what makes The Unfaithful's Sin cards
 * bite — they occupy draws and are discarded without ever doing anything.
 */
final class TurnManager {

    private final GameState state;
    private final EconomyCalculator economy;

    TurnManager(GameState state, EconomyCalculator economy) {
        this.state = state;
        this.economy = economy;
    }

    /** The maximum hand a player may hold: base hand size plus Idol draws. */
    int handLimit(PlayerState p) {
        return p.profile().baseHandSize() + economy.bonusDrawsFor(p);
    }

    /** DRAW phase: refill the player's hand up to the hand limit. */
    void drawPhase(PlayerState p) {
        state.setPhase(GamePhase.DRAW);
        int limit = handLimit(p);
        int drawn = 0;
        while (p.handCards().size() < limit) {
            Card c = p.drawOne();
            if (c == null) {
                break; // deck and discard both exhausted
            }
            drawn += 1;
        }
        state.log(p.name() + " drew " + drawn + " card(s) (hand "
                + p.handCards().size() + "/" + limit + ")");
    }

    /** YIELD phase: credit per-turn resource production. */
    void yieldPhase(PlayerState p) {
        state.setPhase(GamePhase.YIELD);
        String summary = economy.applyYields(p);
        state.log(p.name() + " yields: " + summary);
    }

    /**
     * DISCARD phase / hand-limit enforcement: every card still in hand is
     * discarded so no player hoards cards across turns.
     */
    void discardPhase(PlayerState p) {
        state.setPhase(GamePhase.DISCARD);
        List<Card> hand = new ArrayList<Card>(p.handCards());
        int count = hand.size();
        for (int i = 0; i < hand.size(); i++) {
            Card c = hand.get(i);
            p.removeFromHand(c.id());
            p.discard().add(c);
        }
        if (count > 0) {
            state.log(p.name() + " discarded " + count + " leftover card(s)");
        }
    }

    /** END phase marker. */
    void endPhase(PlayerState p) {
        state.setPhase(GamePhase.END);
    }
}
