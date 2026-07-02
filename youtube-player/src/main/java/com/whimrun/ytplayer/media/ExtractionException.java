package com.whimrun.ytplayer.media;

/**
 * Thrown when yt-dlp cannot produce a playable stream URL.
 *
 * <p>Carries a {@link Kind} so the UI can show a targeted, human-readable
 * message instead of dumping a raw stack trace. The distinction matters:
 * "yt-dlp not found" is a setup problem the user can fix, while
 * "DRM-protected" is a hard limitation of this stack.
 */
public final class ExtractionException extends Exception {

    public enum Kind {
        /** yt-dlp binary is not installed / not on PATH. */
        YT_DLP_NOT_FOUND,
        /** The video exists but no non-DRM, playable format was offered. */
        NO_PLAYABLE_FORMAT,
        /** Video is unavailable, private, removed, or geo/age blocked. */
        VIDEO_UNAVAILABLE,
        /** Video is Widevine/DRM protected — cannot be played by libVLC. */
        DRM_PROTECTED,
        /** Network failure while contacting YouTube. */
        NETWORK_ERROR,
        /** yt-dlp ran but failed for a reason we could not classify. */
        UNKNOWN
    }

    private final Kind kind;

    public ExtractionException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public ExtractionException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

    /** A short, user-facing label suitable for the status bar. */
    public String userMessage() {
        switch (kind) {
            case YT_DLP_NOT_FOUND:
                return "yt-dlp not found on PATH — install it and retry.";
            case NO_PLAYABLE_FORMAT:
                return "No playable format available for this video.";
            case VIDEO_UNAVAILABLE:
                return "Video unavailable (private, removed, or region/age blocked).";
            case DRM_PROTECTED:
                return "DRM-protected, cannot play.";
            case NETWORK_ERROR:
                return "Network error while contacting YouTube.";
            case UNKNOWN:
            default:
                return "Extraction failed: " + getMessage();
        }
    }
}
