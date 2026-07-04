package com.rampart.engine;

import com.rampart.model.GameState;
import com.rampart.model.Rules;
import com.rampart.model.Ship;

/**
 * Applies scoring rules to a {@link GameState}. Consumes and mutates the state's
 * running score via {@link GameState#addScore(long)}, and reads {@link Ship} type
 * values and the {@link com.rampart.model.Grid} enclosed-cell count. Round
 * progression itself (round number, ship refill, cannon pool) is owned by the
 * {@link GameEngine}; this class only awards points.
 */
public final class ScoreSystem {

    /**
     * Awards the score value of a sunk {@link Ship}.
     *
     * @param state the live {@link GameState}
     * @param ship  the {@link Ship} that was just destroyed
     */
    public void onShipSank(GameState state, Ship ship) {
        state.addScore(ship.type().scoreValue());
    }

    /**
     * Awards end-of-round points: {@link Rules#SCORE_PER_TERRITORY_CELL} per
     * currently enclosed {@link com.rampart.model.Grid} cell plus the flat
     * {@link Rules#SCORE_ROUND_SURVIVAL_BONUS}. Called by the {@link GameEngine} when
     * a REPAIR phase ends with the survival rule satisfied.
     *
     * @param state the live {@link GameState}
     * @return the total points awarded
     */
    public long scoreRoundSurvival(GameState state) {
        int cells = TerritoryCalculator.countEnclosedCells(state.gridModel());
        long award = (long) cells * Rules.SCORE_PER_TERRITORY_CELL + Rules.SCORE_ROUND_SURVIVAL_BONUS;
        state.addScore(award);
        return award;
    }
}
