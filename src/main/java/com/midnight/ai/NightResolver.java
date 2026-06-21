package com.midnight.ai;

import com.midnight.core.GameState;

/**
 * The hook {@link com.midnight.core.GameState#endDay(NightResolver)} calls to run
 * Doomdark's entire NIGHT phase against the live state: move his armies, resolve
 * combats, and capture or lose strongholds. Implementations mutate {@code state}
 * through core setters, return a human-readable {@link NightReport}, and must NOT
 * advance the day (the engine does that afterward). Never returns null.
 *
 * <p>Owned by Task 2 ({@code com.midnight.ai}); declared here only so the core's
 * {@code endDay} signature can compile and be tested in isolation.
 */
public interface NightResolver {
    NightReport resolveNight(GameState state);
}
