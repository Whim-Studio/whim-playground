package com.midnight.ai;

import com.midnight.core.GameState;
import com.midnight.core.Location;

/**
 * Resolves a single battle between all forces present at one location.
 *
 * <p>Implementations apply casualties to the colocated characters and any
 * stronghold via the {@code com.midnight.core} setters and return the
 * {@link BattleResult}. Combat math MUST factor the terrain's defensive bonus,
 * each lord's courage, and fatigue (energy).
 */
public interface CombatResolver {

    /**
     * Resolve a battle between all forces present at {@code where}, applying
     * casualties and any stronghold capture via core setters. Never returns
     * {@code null}.
     */
    BattleResult resolveBattle(GameState state, Location where);
}
