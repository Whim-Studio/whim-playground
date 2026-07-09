package com.whim.alganon.ui;

/**
 * No-op sound seam. The clean-room build ships zero audio assets, but the UI still wants a
 * single place to signal "something happened" so a future audio layer can subscribe without
 * touching call sites. Every method is a stub; swap the body for a real player later.
 *
 * <p>[Gap — my design] There is no audio requirement in the contract; this exists purely so
 * button clicks / ability casts / level-ups have a named hook instead of being silent holes.</p>
 */
public final class SoundHooks {

    /** Logical sound cues the UI can fire. Kept coarse on purpose. */
    public enum Cue {
        UI_CLICK, UI_BACK, WIZARD_STEP, GAME_START,
        ABILITY_CAST, ABILITY_FAIL, HIT, LEVEL_UP, LOOT, QUEST_UPDATE, ERROR
    }

    private static SoundHooks instance = new SoundHooks();

    private boolean muted = false;

    private SoundHooks() {}

    public static SoundHooks get() { return instance; }

    public boolean isMuted() { return muted; }

    public void setMuted(boolean m) { this.muted = m; }

    /** Fire a cue. No-op today; intentionally cheap so callers can pepper it freely. */
    public void play(Cue cue) {
        if (muted || cue == null) return;
        // Intentionally silent: no audio files in the clean-room build.
    }
}
