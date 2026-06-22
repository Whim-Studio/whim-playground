package com.whim.babylon5.domain;

/**
 * Physical zones a card can occupy for a single player.
 *
 *  - DRAW_DECK    : the player's play deck (index 0 == top).
 *  - HAND         : cards in hand.
 *  - INNER_CIRCLE : the Ambassador's row (rulebook "Inner Circle").
 *  - SUPPORTING   : the Supporting Cards row below the Ambassador.
 *  - DISCARD      : the discard pile.
 *  - REMOVED      : removed from the game.
 */
public enum ZoneType { DRAW_DECK, HAND, INNER_CIRCLE, SUPPORTING, DISCARD, REMOVED }
