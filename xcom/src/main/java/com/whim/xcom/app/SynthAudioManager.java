package com.whim.xcom.app;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * A tiny, asset-free {@link AudioManager} that synthesises short beeps with
 * {@code javax.sound.sampled} — no copyrighted audio, no files. The pitch is
 * derived from the sound id so different cues sound different. Every operation is
 * best-effort: if no audio device is available (headless/CI) it silently no-ops,
 * so it is always safe to install.
 */
public final class SynthAudioManager implements AudioManager {

    private static final float SAMPLE_RATE = 22050f;

    private volatile boolean muted;
    private volatile float masterVolume = 0.5f;

    @Override
    public void playSfx(final String soundId) {
        if (muted || masterVolume <= 0f) {
            return;
        }
        final int freq = 220 + Math.abs(soundId == null ? 0 : soundId.hashCode()) % 660;
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                beep(freq, 90);
            }
        }, "sfx");
        t.setDaemon(true);
        t.start();
    }

    private void beep(int freqHz, int millis) {
        try {
            AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
            SourceDataLine line = AudioSystem.getSourceDataLine(fmt);
            line.open(fmt);
            line.start();
            int samples = (int) (SAMPLE_RATE * millis / 1000);
            byte[] buf = new byte[samples];
            for (int i = 0; i < samples; i++) {
                double angle = 2.0 * Math.PI * i * freqHz / SAMPLE_RATE;
                double envelope = 1.0 - (double) i / samples; // fade out
                buf[i] = (byte) (Math.sin(angle) * 90 * masterVolume * envelope);
            }
            line.write(buf, 0, buf.length);
            line.drain();
            line.stop();
            line.close();
        } catch (Exception ignored) {
            // No audio device (headless/CI) — silently ignore.
        }
    }

    @Override public void playMusic(String trackId) { /* not implemented in the slice */ }
    @Override public void stopMusic() { /* no-op */ }

    @Override public void setMasterVolume(float volume) {
        masterVolume = Math.max(0f, Math.min(1f, volume));
    }

    @Override public boolean isMuted() { return muted; }
    @Override public void setMuted(boolean muted) { this.muted = muted; }
}
