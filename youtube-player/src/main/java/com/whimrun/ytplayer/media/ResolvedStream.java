package com.whimrun.ytplayer.media;

/**
 * The result of resolving a YouTube URL to direct, libVLC-playable stream URLs.
 *
 * <p>We prefer adaptive formats (separate video + audio) so we can reach 1080p —
 * YouTube's muxed/progressive formats top out around 720p. When yt-dlp returns
 * an adaptive pair, {@link #videoUrl} is the video track and {@link #audioUrl}
 * is the audio track (fed to libVLC as an input-slave). When it returns a single
 * muxed URL, {@link #audioUrl} is {@code null}.
 */
public final class ResolvedStream {

    private final String videoUrl;
    private final String audioUrl; // null when the video URL is already muxed
    private final String title;

    public ResolvedStream(String videoUrl, String audioUrl, String title) {
        this.videoUrl = videoUrl;
        this.audioUrl = audioUrl;
        this.title = title;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    /** @return the separate audio track URL, or {@code null} for muxed streams. */
    public String getAudioUrl() {
        return audioUrl;
    }

    public boolean hasSeparateAudio() {
        return audioUrl != null && !audioUrl.isEmpty();
    }

    public String getTitle() {
        return title;
    }
}
