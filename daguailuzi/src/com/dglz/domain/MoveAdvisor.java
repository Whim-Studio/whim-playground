package com.dglz.domain;

import com.dglz.engine.GameState;

/** Coach advisor. Implemented by Task 2, used by Task 3 UI. */
public interface MoveAdvisor {
    MoveSuggestion advise(GameState state, int humanSeat);
}
