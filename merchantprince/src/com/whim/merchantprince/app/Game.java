package com.whim.merchantprince.app;

import com.whim.merchantprince.data.WorldFactory;
import com.whim.merchantprince.engine.Constants;
import com.whim.merchantprince.engine.Rng;
import com.whim.merchantprince.model.GameState;

/**
 * Shared application context passed to every screen: the current {@link GameState},
 * the RNG, and the {@link ScreenManager} for navigation. A few transient "hand-off"
 * fields let one screen focus another on a specific city or unit without coupling
 * the screens to each other.
 */
public class Game {

    public GameState state;
    public final Rng rng = new Rng();
    public final ScreenManager screens = new ScreenManager();

    /** CardLayout keys — one per screen. */
    public static final String MENU    = "menu";
    public static final String NEWGAME = "newgame";
    public static final String MAP     = "map";
    public static final String MARKET  = "market";
    public static final String FLEET   = "fleet";
    public static final String VENICE  = "venice";
    public static final String GAMEOVER = "gameover";

    // ---- transient hand-off context ------------------------------------
    /** City the Market/Fleet screen should focus on next. */
    public int focusCityId = 0;
    /** Unit the Fleet screen should focus on next, or -1. */
    public int focusUnitId = -1;
    /** Card to return to after a sub-screen (defaults to the map). */
    public String returnTo = MAP;

    /** Build a brand-new game with the given player surname, crest and length. */
    public void newGame(String surname, int crestColor, int endYear) {
        state = WorldFactory.newGame(surname, crestColor, endYear, rng);
    }

    public int defaultEndYear() { return Constants.DEFAULT_END_YEAR; }
}
