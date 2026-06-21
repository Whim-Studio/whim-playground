package com.midnight.ai;

import com.midnight.core.GameState;

/**
 * The hook {@code GameState.endDay} calls to run Doomdark's NIGHT phase.
 *
 * <p>An implementation moves Doomdark's armies, resolves every combat, and
 * captures or loses strongholds, mutating {@code state} through the
 * {@code com.midnight.core} setters. It MUST NOT advance the day &mdash;
 * {@link GameState#endDay(NightResolver)} does that immediately afterward &mdash;
 * and it MUST NOT move FREE lords (they are frozen at night). It never returns
 * {@code null}.
 */
public interface NightResolver {

    /**
     * Run Doomdark's entire NIGHT phase against the live {@code state} and return
     * a human-readable report of what happened. Never returns {@code null}.
     */
    NightReport resolveNight(GameState state);
}
