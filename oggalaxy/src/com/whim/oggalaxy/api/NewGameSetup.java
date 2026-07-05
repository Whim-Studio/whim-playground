package com.whim.oggalaxy.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything the start screen collects to create a new game. The engine resolves any
 * {@link Ids.Difficulty#RANDOM} opponents to a concrete level at creation using the seed
 * and logs which level was picked.
 */
public final class NewGameSetup implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Configuration for one AI opponent. */
    public static final class AIConfig implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String name;            // may be null -> engine assigns a name
        public final Ids.Difficulty difficulty;

        public AIConfig(String name, Ids.Difficulty difficulty) {
            this.name = name;
            this.difficulty = difficulty;
        }
    }

    public final String commanderName;
    public final Ids.PlayerClass playerClass;
    public final List<AIConfig> opponents;
    public final long seed;

    public NewGameSetup(String commanderName, Ids.PlayerClass playerClass,
                        List<AIConfig> opponents, long seed) {
        this.commanderName = commanderName;
        this.playerClass = playerClass;
        this.opponents = opponents == null ? new ArrayList<AIConfig>() : opponents;
        this.seed = seed;
    }
}
