package com.whim.tippingpoint.engine;

import com.whim.tippingpoint.domain.CitizenType;
import com.whim.tippingpoint.domain.GameState;
import com.whim.tippingpoint.domain.Player;

/**
 * The authoritative rules engine for a game of Tipping Point. All rule enforcement,
 * phase resolution, AI play, and win/loss detection live here. The UI reads state from
 * {@code domain} objects and mutates the game exclusively through this interface.
 */
public interface GameEngine {
    GameState state();

    // ----- Development Phase -----

    /** Grants {@code cashFlow} income to the player whose sub-turn is starting, as needed. */
    void beginDevelopmentPhase();

    boolean canBuyDevelopment(Player p, int row, int col);

    /** Validates, deducts cash, applies the card's Status-Board deltas, and refills the slot. */
    void buyDevelopment(Player p, int row, int col);

    boolean canRecruit(Player p, CitizenType type);

    /** Validates the cost, adds the citizen; a farmer raises {@code foodProduction}. */
    void recruit(Player p, CitizenType type);

    /** Advances to the next player; when all have acted, refills the market and moves to the Weather Phase. */
    void endDevelopmentTurn();

    boolean developmentPhaseComplete();

    // ----- AI -----

    /** Plays a full Development sub-turn for an AI player, then ends the turn. */
    void runAiDevelopmentTurn(Player p);

    // ----- Weather Phase -----

    /** Feeds citizens, clears/reveals/resolves weather, advances the timeline, and sets up the next round. */
    WeatherReport resolveWeatherPhase();

    // ----- Win/loss -----

    WinStatus checkStatus();
}
