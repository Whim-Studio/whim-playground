package com.whim.ytmeta.logic;

import com.whim.ytmeta.model.VideoMetadata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Networking + extraction engine. Uses only {@link HttpURLConnection} and
 * {@link BufferedReader} to fetch the raw HTML of a YouTube watch page, then
 * isolates the relevant fields with {@link Pattern} matching against the
 * embedded Open Graph / schema.org metadata and the bundled player JSON.
 *
 * No external libraries are used; all HTML isolation is native.
 */
public final class YouTubeMetadataExtractor {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 20000;

    // --- Title -------------------------------------------------------------
    private static final Pattern OG_TITLE =
            Pattern.compile("<meta\\s+property=\"og:title\"\\s+content=\"(.*?)\"", Pattern.DOTALL);
    private static final Pattern JSON_TITLE =
            Pattern.compile("\"title\":\\s*\\{\\s*\"simpleText\":\\s*\"(.*?)\"", Pattern.DOTALL);

    // --- Channel / uploader ------------------------------------------------
    private static final Pattern LINK_AUTHOR =
            Pattern.compile("<link\\s+itemprop=\"name\"\\s+content=\"(.*?)\"", Pattern.DOTALL);
    private static final Pattern JSON_AUTHOR =
            Pattern.compile("\"author\":\\s*\"(.*?)\"", Pattern.DOTALL);
    private static final Pattern JSON_OWNER =
            Pattern.compile("\"ownerChannelName\":\\s*\"(.*?)\"", Pattern.DOTALL);

    // --- Upload date -------------------------------------------------------
    private static final Pattern META_UPLOAD_DATE =
            Pattern.compile("<meta\\s+itemprop=\"uploadDate\"\\s+content=\"(.*?)\"", Pattern.DOTALL);
    private static final Pattern JSON_UPLOAD_DATE =
            Pattern.compile("\"uploadDate\":\\s*\"(.*?)\"", Pattern.DOTALL);
    private static final Pattern JSON_PUBLISH_DATE =
            Pattern.compile("\"publishDate\":\\s*\"(.*?)\"", Pattern.DOTALL);

    // --- Duration ----------------------------------------------------------
    private static final Pattern META_DURATION =
            Pattern.compile("<meta\\s+itemprop=\"duration\"\\s+content=\"(PT.*?)\"", Pattern.DOTALL);
    private static final Pattern JSON_LENGTH_SECONDS =
            Pattern.compile("\"lengthSeconds\":\\s*\"(\\d+)\"", Pattern.DOTALL);

    private static final Pattern ISO_DURATION =
            Pattern.compile("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?");

    /**
     * Fetches the page for {@code videoUrl} and returns a populated
     * {@link VideoMetadata}. Fields that cannot be located are left blank
     * rather than failing the whole extraction.
     *
     * @throws IOException if the page cannot be fetched.
     */
    public VideoMetadata extract(String videoUrl) throws IOException {
        String trimmed = videoUrl == null ? "" : videoUrl.trim();
        if (trimmed.isEmpty()) {
            throw new IOException("Please enter a YouTube URL.");
        }

        String html = fetchHtml(trimmed);

        String title = firstMatch(html, OG_TITLE, JSON_TITLE);
        String channel = firstMatch(html, LINK_AUTHOR, JSON_OWNER, JSON_AUTHOR);
        String uploadDateRaw = firstMatch(html, META_UPLOAD_DATE, JSON_UPLOAD_DATE, JSON_PUBLISH_DATE);
        String uploadDate = normalizeUploadDate(uploadDateRaw);

        String duration = extractDuration(html);

        return new VideoMetadata(
                currentTimestamp(),
                trimmed,
                uploadDate,
                duration,
                unescape(channel),
                unescape(title));
    }

    /** Reads the raw HTML over HTTP(S) using a browser-like user-agent. */
    private String fetchHtml(String videoUrl) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(videoUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");

            int status = connection.getResponseCode();
            if (status < 200 || status >= 400) {
                throw new IOException("Server returned HTTP " + status
                        + " for the requested URL.");
            }

            InputStream stream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, Charset.forName("UTF-8")));
            try {
                StringBuilder builder = new StringBuilder(1 << 16);
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }
                return builder.toString();
            } finally {
                reader.close();
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /** Returns the first capturing group from the first matching pattern. */
    private static String firstMatch(String html, Pattern... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            Matcher matcher = patterns[i].matcher(html);
            if (matcher.find()) {
                String value = matcher.group(1);
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
        }
        return "";
    }

    /** Locates the duration as ISO 8601 or raw seconds and formats HH:MM:SS. */
    private String extractDuration(String html) {
        Matcher isoMatcher = META_DURATION.matcher(html);
        if (isoMatcher.find()) {
            return isoDurationToHms(isoMatcher.group(1));
        }
        Matcher secondsMatcher = JSON_LENGTH_SECONDS.matcher(html);
        if (secondsMatcher.find()) {
            try {
                long totalSeconds = Long.parseLong(secondsMatcher.group(1));
                return secondsToHms(totalSeconds);
            } catch (NumberFormatException ignored) {
                // fall through to blank
            }
        }
        return "";
    }

    /** Converts an ISO 8601 period such as PT1H23M45S to "01:23:45". */
    public static String isoDurationToHms(String iso) {
        if (iso == null) {
            return "";
        }
        Matcher matcher = ISO_DURATION.matcher(iso.trim());
        if (!matcher.matches()) {
            return "";
        }
        long hours = parseGroup(matcher.group(1));
        long minutes = parseGroup(matcher.group(2));
        long seconds = parseGroup(matcher.group(3));
        long total = hours * 3600 + minutes * 60 + seconds;
        return secondsToHms(total);
    }

    private static long parseGroup(String group) {
        return group == null ? 0L : Long.parseLong(group);
    }

    /** Formats a total number of seconds as zero-padded HH:MM:SS. */
    public static String secondsToHms(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /** Reduces an upload date string to the YYYY-MM-DD prefix. */
    private static String normalizeUploadDate(String raw) {
        if (raw == null || raw.length() < 10) {
            return raw == null ? "" : raw;
        }
        String candidate = raw.substring(0, 10);
        // Validate the YYYY-MM-DD shape; otherwise keep the original token.
        if (candidate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return candidate;
        }
        return raw;
    }

    /** Records the instant the extraction was processed, including seconds. */
    public static String currentTimestamp() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(new Date());
    }

    /**
     * Decodes the small set of HTML / unicode escapes that appear inside the
     * embedded attributes and JSON so the spreadsheet row reads cleanly.
     */
    private static String unescape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String result = value
                .replace("\\u0026", "&")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("\\\"", "\"")
                .replace("\\/", "/");
        // Strip any stray tabs/newlines so a single spreadsheet row stays intact.
        return result.replace("\t", " ").replace("\r", " ").replace("\n", " ").trim();
    }
}
