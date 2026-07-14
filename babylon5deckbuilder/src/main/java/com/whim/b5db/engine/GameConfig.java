package com.whim.b5db.engine;

/** Tunable rules constants (see GDD Appendix A.9). */
public final class GameConfig {

    public int handSize = 5;
    public int prestigeTarget = 40;
    public int maxTurns = 200;
    /** Copies of each unique market card seeded into the RIM deck. */
    public int rimCopies = 3;
    /** Hard cap on unique market cards (GDD constraint). */
    public int maxUniqueMarketCards = 150;

    public GameConfig() {
    }

    public GameConfig(int prestigeTarget) {
        this.prestigeTarget = prestigeTarget;
    }
}
