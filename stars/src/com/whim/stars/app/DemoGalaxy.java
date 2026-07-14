package com.whim.stars.app;

import com.whim.stars.model.Galaxy;

/**
 * Convenience builder for the default startup galaxy. Delegates to
 * {@link GalaxyFactory} with the demo {@link GameSetup} so there is a single
 * galaxy-generation code path.
 */
public final class DemoGalaxy {

    /** Player id of the human in every galaxy this app builds. */
    public static final int HUMAN_ID = GalaxyFactory.HUMAN_ID;

    private DemoGalaxy() {
    }

    /** The default demo galaxy (fixed seed → reproducible). */
    public static Galaxy build() {
        return GalaxyFactory.build(GameSetup.demo());
    }

    /** A demo galaxy with a specific seed (used by "New Game" quick-restart). */
    public static Galaxy build(long seed) {
        GameSetup setup = GameSetup.demo();
        setup.seed = seed;
        return GalaxyFactory.build(setup);
    }
}
