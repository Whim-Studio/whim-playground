package com.whim.settlers.engine;

import com.whim.settlers.io.MapLoader;
import com.whim.settlers.map.MapGenerator;
import com.whim.settlers.map.TileMap;

import java.nio.file.Paths;

/**
 * Editable configuration for the new-game / free-play setup screen: which map to
 * play (a generated seed or the hand-built tutorial valley), how many players
 * (2–4 total, one human + the rest AI), and each AI's personality on a
 * peaceful↔aggressive slider. {@link Game} reads this to build the {@link World}.
 */
public final class SetupConfig {

    /** Bundled scenario shipped with the game. */
    public static final String TUTORIAL_MAP = "maps/tutorial-valley.map";

    private static final int GEN_W = 80, GEN_H = 80;
    private static final int MIN_PLAYERS = 2, MAX_PLAYERS = 4;

    private boolean tutorialMap;         // false = generated
    private long seed = 1993L;
    private int players = 2;             // total, incl. the human
    /** Aggression per AI slot (index 0 = AI player 1, …); range [0,1]. */
    private final float[] aggression = { 0.5f, 0.7f, 0.3f };

    // --- map ---
    public boolean tutorialMap()      { return tutorialMap; }
    public void setTutorialMap(boolean v) { this.tutorialMap = v; }
    public long seed()                { return seed; }
    public void bumpSeed(int d)       { seed = Math.max(0, seed + d); }

    /** Build the chosen map. Falls back to a generated map if the file is missing. */
    public TileMap buildMap() {
        if (tutorialMap) {
            try {
                return MapLoader.fromFile(Paths.get(TUTORIAL_MAP));
            } catch (Exception e) {
                try { return MapLoader.fromResource("/" + TUTORIAL_MAP); }
                catch (Exception ignored) { /* fall through to generated */ }
            }
        }
        return MapGenerator.generate(GEN_W, GEN_H, seed);
    }

    // --- players ---
    public int players()              { return players; }
    public int aiCount()              { return players - 1; }
    public void bumpPlayers(int d) {
        players = Math.max(MIN_PLAYERS, Math.min(MAX_PLAYERS, players + d));
    }

    public float aggression(int aiIndex) {
        return (aiIndex >= 0 && aiIndex < aggression.length) ? aggression[aiIndex] : 0.5f;
    }
    public void bumpAggression(int aiIndex, float d) {
        if (aiIndex < 0 || aiIndex >= aggression.length) return;
        float v = aggression[aiIndex] + d;
        aggression[aiIndex] = v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    /** Aggression values for the active AI slots, as {@link World#spawnAiPlayers} wants. */
    public float[] aiAggressions() {
        float[] out = new float[aiCount()];
        for (int i = 0; i < out.length; i++) out[i] = aggression(i);
        return out;
    }

    /** Human-readable personality label for a slider value. */
    public static String personalityLabel(float aggression) {
        if (aggression < 0.2f) return "Peaceful";
        if (aggression < 0.4f) return "Cautious";
        if (aggression < 0.6f) return "Balanced";
        if (aggression < 0.8f) return "Assertive";
        return "Aggressive";
    }
}
