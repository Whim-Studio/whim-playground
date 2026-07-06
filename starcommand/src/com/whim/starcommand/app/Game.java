package com.whim.starcommand.app;

import com.whim.starcommand.engine.CharacterGen;
import com.whim.starcommand.engine.Content;
import com.whim.starcommand.engine.GalaxyGen;
import com.whim.starcommand.engine.Rng;
import com.whim.starcommand.model.Character;
import com.whim.starcommand.model.GameState;

/**
 * Shared application context passed to every screen: the current game state,
 * RNG and engine services, plus the screen manager for navigation.
 */
public class Game {

    public GameState state;
    public final Rng rng = new Rng();
    public final CharacterGen charGen = new CharacterGen(rng);
    public final ScreenManager screens = new ScreenManager();

    /** Names used as CardLayout keys. */
    public static final String MENU     = "menu";
    public static final String CREATE   = "create";
    public static final String STARPORT = "starport";
    public static final String GALAXY   = "galaxy";
    public static final String COMBAT   = "combat";
    public static final String GROUND   = "ground";
    public static final String AREA     = "area";
    public static final String HELP     = "help";

    /** Start a fresh game with an empty crew roster (to be filled in char creation). */
    public void newGame() {
        state = new GameState();
        state.credits = 5000;
        state.ship = Content.startingShip();
        new GalaxyGen(rng).generate(state);
    }

    /** Best available gunner, used for combat accuracy bonuses. */
    public Character captain() {
        Character best = null;
        if (state == null) return null;
        for (Character c : state.crew) {
            if (!c.alive) continue;
            if (best == null || c.accuracy > best.accuracy) best = c;
        }
        return best;
    }
}
