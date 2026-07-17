package com.whim.xcom.app;

/**
 * Audio seam. Phase 0 ships only {@link NoopAudioManager}; a real backend
 * (WAV/OGG via {@code javax.sound.sampled} or a library) can be dropped in later
 * without touching call sites. Sound cues are addressed by string id so the
 * ruleset/data layer can name them.
 */
public interface AudioManager {

    /** Play a one-shot sound effect by id (e.g. {@code "ufo_detected"}). */
    void playSfx(String soundId);

    /** Start (or switch to) a looping music track by id. */
    void playMusic(String trackId);

    void stopMusic();

    /** Master volume 0.0..1.0. */
    void setMasterVolume(float volume);

    boolean isMuted();

    void setMuted(boolean muted);
}
