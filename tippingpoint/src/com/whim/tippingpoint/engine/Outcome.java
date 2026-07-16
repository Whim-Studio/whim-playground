package com.whim.tippingpoint.engine;

/**
 * Terminal (or in-progress) result of a game, as decided by {@link GameEngine#checkStatus()}.
 *
 * <ul>
 *   <li>{@link #IN_PROGRESS} — no victory/defeat condition met yet.</li>
 *   <li>{@link #PLAYER_WIN} — COMPETITIVE mode: a single player has won.</li>
 *   <li>{@link #TEAM_WIN} — COOPERATIVE mode: every player reached the target by the end year.</li>
 *   <li>{@link #TEAM_LOSS_TIPPING} — global CO2 crossed the tipping point; everyone loses.</li>
 *   <li>{@link #TEAM_LOSS_TIME} — COOPERATIVE mode: the end year arrived without all players winning.</li>
 * </ul>
 */
public enum Outcome {
    IN_PROGRESS,
    PLAYER_WIN,
    TEAM_WIN,
    TEAM_LOSS_TIPPING,
    TEAM_LOSS_TIME
}
