package com.whim.stars.app;

import com.whim.stars.model.Galaxy;
import com.whim.stars.model.race.PRT;

/**
 * User-chosen options for a new game, produced by the New Game wizard and
 * consumed by {@link GalaxyFactory}. Plain mutable fields keep the dialog code
 * simple; {@link #demo()} supplies the defaults used at startup.
 */
public final class GameSetup {

    public Galaxy.UniverseSize size = Galaxy.UniverseSize.SMALL;
    public String humanRaceName = "Human";
    public PRT humanPrt = PRT.JOAT;
    public int aiOpponents = 1;
    public long seed = 20250714L;

    public GameSetup() {
    }

    /** The default startup scenario: a small galaxy with one AI rival. */
    public static GameSetup demo() {
        return new GameSetup();
    }
}
