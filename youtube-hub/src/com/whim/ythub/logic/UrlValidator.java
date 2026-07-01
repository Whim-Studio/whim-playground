package com.whim.ythub.logic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stateless validation helper for YouTube video URLs.
 *
 * <p>Part of the logic layer ({@code com.whim.ythub.logic}). It has no Swing,
 * file, or networking dependencies so it can be unit-tested and reused freely.
 * Written for strict Java 8 (no {@code var}, no text blocks, no post-8 APIs).</p>
 *
 * <p>Accepted forms (host is matched case-insensitively):</p>
 * <ul>
 *   <li>{@code https://www.youtube.com/watch?v=VIDEOID}</li>
 *   <li>{@code http(s)://youtube.com/watch?v=VIDEOID}</li>
 *   <li>{@code https://m.youtube.com/watch?v=VIDEOID}</li>
 *   <li>{@code https://youtu.be/VIDEOID}</li>
 * </ul>
 * plus optional trailing query parameters (e.g. {@code &t=30s}, playlist ids).
 * Video ids are exactly 11 characters of {@code [A-Za-z0-9_-]}.
 */
public final class UrlValidator {

    /** 11-character YouTube video id character class. */
    private static final String VIDEO_ID = "[A-Za-z0-9_-]{11}";

    /**
     * {@code watch?v=} style URLs on the youtube.com family of hosts.
     *
     * <p>The host is anchored so only {@code youtube.com}, {@code www.youtube.com}
     * and {@code m.youtube.com} are accepted; the video id must be the first
     * query parameter (or appear as {@code &v=}), followed by an optional query
     * string. Anchoring the host prevents look-alikes such as
     * {@code evil-youtube.com} or {@code youtube.com.evil.com} from matching.</p>
     */
    private static final Pattern WATCH_PATTERN = Pattern.compile(
            "^https?://(?:www\\.|m\\.)?youtube\\.com/watch\\?"
                    + "(?:[^#]*&)?v=(" + VIDEO_ID + ")(?:[&#].*)?$",
            Pattern.CASE_INSENSITIVE);

    /** Short {@code youtu.be/ID} style URLs. */
    private static final Pattern SHORT_PATTERN = Pattern.compile(
            "^https?://youtu\\.be/(" + VIDEO_ID + ")(?:[/?#].*)?$",
            Pattern.CASE_INSENSITIVE);

    private UrlValidator() {
        // static-only utility
    }

    /**
     * Returns {@code true} when {@code url} is a well-formed, recognised YouTube
     * video link. Null, empty and non-YouTube inputs return {@code false}.
     *
     * @param url the candidate URL (may be {@code null})
     * @return {@code true} if the URL is a valid YouTube video link
     */
    public static boolean isValidYouTubeUrl(String url) {
        if (url == null) {
            return false;
        }
        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        return WATCH_PATTERN.matcher(trimmed).matches()
                || SHORT_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Extracts the 11-character video id from a valid YouTube URL, or returns
     * {@code null} when the URL is not a recognised YouTube video link.
     *
     * @param url the candidate URL (may be {@code null})
     * @return the video id, or {@code null} if none could be extracted
     */
    public static String extractVideoId(String url) {
        if (url == null) {
            return null;
        }
        String trimmed = url.trim();
        Matcher watch = WATCH_PATTERN.matcher(trimmed);
        if (watch.matches()) {
            return watch.group(1);
        }
        Matcher shortMatch = SHORT_PATTERN.matcher(trimmed);
        if (shortMatch.matches()) {
            return shortMatch.group(1);
        }
        return null;
    }

    /**
     * Human-readable explanation of why {@code url} is (in)valid, suitable for
     * display in the UI. Returns {@code null} when the URL is valid.
     *
     * @param url the candidate URL (may be {@code null})
     * @return an error reason, or {@code null} if the URL is valid
     */
    public static String describe(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "Please enter a URL.";
        }
        String trimmed = url.trim();
        if (isValidYouTubeUrl(trimmed)) {
            return null;
        }
        String lower = trimmed.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return "URL must start with http:// or https://.";
        }
        if (!lower.contains("youtube.com") && !lower.contains("youtu.be")) {
            return "That does not look like a YouTube link.";
        }
        return "Not a recognised YouTube video URL (expected an 11-character video id).";
    }
}
