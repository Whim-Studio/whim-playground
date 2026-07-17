package com.whim.xcom.app;

/**
 * Silent {@link AudioManager} used until a real audio backend lands. Keeps a
 * mute/volume state so UI wiring can be built and tested now.
 */
public final class NoopAudioManager implements AudioManager {

    private float masterVolume = 1.0f;
    private boolean muted;

    @Override public void playSfx(String soundId) { /* no-op */ }
    @Override public void playMusic(String trackId) { /* no-op */ }
    @Override public void stopMusic() { /* no-op */ }

    @Override
    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    public float masterVolume() {
        return masterVolume;
    }

    @Override public boolean isMuted() { return muted; }
    @Override public void setMuted(boolean muted) { this.muted = muted; }
}
