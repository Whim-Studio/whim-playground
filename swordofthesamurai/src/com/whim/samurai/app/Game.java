package com.whim.samurai.app;

import com.whim.samurai.engine.Rng;
import com.whim.samurai.engine.WorldGen;
import com.whim.samurai.model.GameState;
import com.whim.samurai.model.Province;
import com.whim.samurai.model.Rival;

/**
 * Shared application context passed to every screen: the current {@link GameState},
 * the RNG, and the {@link ScreenManager} for navigation. A handful of transient
 * "hand-off" fields let one screen launch an action sequence on another (e.g. the
 * map launches a duel against a specific rival) without coupling the screens.
 */
public class Game {

    public GameState state;
    public final Rng rng = new Rng();
    public final ScreenManager screens = new ScreenManager();

    /** CardLayout keys — one per screen. */
    public static final String MENU      = "menu";
    public static final String CREATE    = "create";
    public static final String MAP       = "map";
    public static final String CHARACTER = "character";
    public static final String FAMILY    = "family";
    public static final String DUEL      = "duel";
    public static final String BATTLE    = "battle";
    public static final String NINJA     = "ninja";
    public static final String ENCOUNTER = "encounter";
    public static final String GAMEOVER  = "gameover";
    public static final String HELP      = "help";

    // ---- transient hand-off context (set by launcher, read by the action screen) ----
    public Rival duelTarget;          // rival to duel next
    public String duelReason = "";    // flavour + honor implications
    public boolean duelToDeath = false;
    public Province battleTarget;     // province to assault next
    public Province ninjaTarget;      // province/castle to infiltrate next
    public String afterAction = MAP;  // card to return to after an action sequence

    /** Build a brand-new game world with the given player/clan name. */
    public void newGame(String playerName, String clanName) {
        state = new WorldGen(rng).generate(playerName, clanName);
    }

    public void returnFromAction() { screens.show(afterAction); }
}
