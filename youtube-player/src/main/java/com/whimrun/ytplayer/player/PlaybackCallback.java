package com.whimrun.ytplayer.player;

/**
 * Callbacks the {@link VideoPlayer} raises as playback progresses.
 *
 * <p><b>Threading contract:</b> every method here is guaranteed to be invoked on
 * the Swing Event Dispatch Thread. {@link VideoPlayer} receives the raw libVLC
 * events on native callback threads and re-posts them via {@code invokeLater},
 * so implementations may freely touch Swing components. This is the single choke
 * point that enforces "native callbacks never touch Swing directly."
 */
public interface PlaybackCallback {

    /** Media parsed and length known (milliseconds). */
    void onLengthKnown(long lengthMillis);

    /** Regular playback progress tick. */
    void onTimeChanged(long timeMillis, long lengthMillis);

    /** Transitioned into the playing state. */
    void onPlaying();

    /** Transitioned into the paused state. */
    void onPaused();

    /** Stopped (either by us or naturally). */
    void onStopped();

    /** Reached end of media. */
    void onFinished();

    /** libVLC reported an error opening/decoding the media. */
    void onError();
}
