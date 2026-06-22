package com.whim.babylon5.domain;

/**
 * Strict turn sequence derived from the rulebook ("Playing the Game" / round
 * structure: READY -> CONFLICT -> ACTION -> AFTERMATH -> DRAW). The contract
 * folds the rulebook's AFTERMATH round into RESOLUTION (Aftermath card play
 * happens in RESOLUTION), giving:
 *
 *   READY -> CONFLICT -> ACTION -> RESOLUTION -> DRAW
 *
 * After DRAW, play passes to the next player and begins their READY.
 */
public enum Phase { READY, CONFLICT, ACTION, RESOLUTION, DRAW }
