package com.whimrun.ytplayer.player;

import java.awt.Component;

import javax.swing.SwingUtilities;

import com.whimrun.ytplayer.media.ResolvedStream;

import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;

/**
 * Thin wrapper around vlcj's {@link EmbeddedMediaPlayerComponent}.
 *
 * <p>{@code EmbeddedMediaPlayerComponent} is an AWT heavyweight component that
 * owns a native video surface (a heavyweight {@code Canvas} under the hood) — the
 * required approach for libVLC to render into a Swing window. We add it directly
 * to the frame.
 *
 * <p>All libVLC events arrive on native threads; this class re-marshals every one
 * onto the EDT before invoking the {@link PlaybackCallback}. Nothing in here or
 * downstream touches Swing off the EDT.
 */
public final class VideoPlayer {

    private final EmbeddedMediaPlayerComponent mediaComponent;
    private final PlaybackCallback callback;

    /**
     * @param callback receives EDT-marshalled playback events; must not be null
     * @throws RuntimeException if the native libVLC library cannot be loaded
     *         (vlcj throws during factory creation). Callers surface this as a
     *         "native VLC library not found" message.
     */
    public VideoPlayer(PlaybackCallback callback) {
        this.callback = callback;
        // Construction triggers MediaPlayerFactory creation and native discovery.
        // If libVLC is missing this throws, and we let it propagate to the caller.
        this.mediaComponent = new EmbeddedMediaPlayerComponent();
        registerEvents();
    }

    /** The heavyweight component to embed in the window. */
    public Component videoSurface() {
        return mediaComponent;
    }

    private MediaPlayer player() {
        return mediaComponent.mediaPlayer();
    }

    /**
     * Start playing a resolved stream. For adaptive streams the separate audio
     * track is attached via libVLC's {@code :input-slave} option so video+audio
     * play in sync.
     *
     * <p>Safe to call from the EDT; the actual media start is dispatched onto a
     * dedicated background thread because vlcj's synchronous {@code play} can
     * briefly block while opening the network stream, and we must never stall the
     * EDT.
     */
    public void play(final ResolvedStream stream) {
        final String videoMrl = stream.getVideoUrl();
        final String[] options = stream.hasSeparateAudio()
                ? new String[] { ":input-slave=" + stream.getAudioUrl() }
                : new String[0];

        Thread starter = new Thread(new Runnable() {
            @Override
            public void run() {
                // media().play(...) prepares and starts asynchronously; events come
                // back through the listener we registered.
                player().media().play(videoMrl, options);
            }
        }, "vlcj-media-start");
        starter.setDaemon(true);
        starter.start();
    }

    /** Toggle between play and pause. No-op if no media is loaded. */
    public void togglePause() {
        player().controls().pause();
    }

    public void resume() {
        player().controls().play();
    }

    public void stop() {
        player().controls().stop();
    }

    /** @param position 0.0 .. 1.0 fraction of total length. */
    public void setPosition(float position) {
        player().controls().setPosition(position);
    }

    /** @param volume 0 .. 100 */
    public void setVolume(int volume) {
        player().audio().setVolume(volume);
    }

    public boolean isPlaying() {
        return player().status().isPlaying();
    }

    public long length() {
        return player().status().length();
    }

    /** Release native resources. Call once, on shutdown, from the EDT. */
    public void release() {
        mediaComponent.release();
    }

    /**
     * Register libVLC event handlers, each of which does nothing but bounce the
     * event onto the EDT. This is the enforcement point for the threading rule.
     */
    private void registerEvents() {
        player().events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void lengthChanged(MediaPlayer mp, final long newLength) {
                post(new Runnable() {
                    public void run() {
                        callback.onLengthKnown(newLength);
                    }
                });
            }

            @Override
            public void timeChanged(MediaPlayer mp, final long newTime) {
                final long len = mp.status().length();
                post(new Runnable() {
                    public void run() {
                        callback.onTimeChanged(newTime, len);
                    }
                });
            }

            @Override
            public void playing(MediaPlayer mp) {
                post(new Runnable() {
                    public void run() {
                        callback.onPlaying();
                    }
                });
            }

            @Override
            public void paused(MediaPlayer mp) {
                post(new Runnable() {
                    public void run() {
                        callback.onPaused();
                    }
                });
            }

            @Override
            public void stopped(MediaPlayer mp) {
                post(new Runnable() {
                    public void run() {
                        callback.onStopped();
                    }
                });
            }

            @Override
            public void finished(MediaPlayer mp) {
                post(new Runnable() {
                    public void run() {
                        callback.onFinished();
                    }
                });
            }

            @Override
            public void error(MediaPlayer mp) {
                post(new Runnable() {
                    public void run() {
                        callback.onError();
                    }
                });
            }
        });
    }

    /** Marshal a runnable onto the EDT (from an arbitrary native callback thread). */
    private static void post(Runnable r) {
        SwingUtilities.invokeLater(r);
    }
}
