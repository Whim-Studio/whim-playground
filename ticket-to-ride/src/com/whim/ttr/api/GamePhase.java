package com.whim.ttr.api;

/**
 * High-level phase of a single game.
 *
 * <ul>
 *   <li>{@link #SETUP} — dealing cards and initial ticket selection.</li>
 *   <li>{@link #PLAYING} — normal turns.</li>
 *   <li>{@link #LAST_ROUND} — triggered once any player drops to &le; 2 trains;
 *       every player (including the trigger) takes exactly one more turn.</li>
 *   <li>{@link #GAME_OVER} — final scoring done.</li>
 * </ul>
 */
public enum GamePhase {
    SETUP, PLAYING, LAST_ROUND, GAME_OVER
}
