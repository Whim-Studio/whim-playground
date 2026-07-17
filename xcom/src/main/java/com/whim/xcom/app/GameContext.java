package com.whim.xcom.app;

import com.whim.xcom.rng.Rng;
import com.whim.xcom.rules.Ruleset;

/**
 * The shared services a screen needs: the active {@link Ruleset}, a seeded
 * {@link Rng} and the {@link AudioManager}. Passed to screens so they never
 * reach for globals — the same seam the Geoscape screen (Phase 2) will use.
 */
public final class GameContext {

    private final Ruleset ruleset;
    private final Rng rng;
    private final AudioManager audio;

    public GameContext(Ruleset ruleset, Rng rng, AudioManager audio) {
        this.ruleset = ruleset;
        this.rng = rng;
        this.audio = audio;
    }

    public Ruleset ruleset() { return ruleset; }
    public Rng rng() { return rng; }
    public AudioManager audio() { return audio; }
}
