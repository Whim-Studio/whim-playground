package com.whim.browser.engine;

import com.whim.browser.model.WebResponse;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Special-case handler for YouTube links.
 *
 * <p><b>Why this exists.</b> The Swing browser renders pages with
 * {@link javax.swing.JEditorPane}, which understands only a limited subset of
 * HTML&nbsp;3.2 and CSS&nbsp;1. It cannot execute JavaScript and cannot play
 * modern (MSE / DASH) video streams, so a YouTube watch page would render as a
 * broken, non-functional shell. Rather than fake playback, the engine
 * intercepts YouTube URLs at the network layer and hands the real link to the
 * operating system's default browser via {@link java.awt.Desktop}. The user
 * still gets a working video; the embedded engine simply reports what it did.</p>
 *
 * <p>Networking here reuses the same {@link HttpURLConnection} pattern,
 * browser-like User-Agent, timeouts and regex-based title extraction as the
 * reference {@code YouTubeMetadataExtractor}, but only to grab the
 * {@code og:title} for a friendly status message — extraction is best-effort
 * and never blocks the hand-off.</p>
 */
public final class YouTubeHandler {

    /** Browser-like agent so YouTube serves the full desktop page. */
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 20000;

    /** Matches youtube.com/watch, youtu.be/, m.youtube.com and /shorts/ links. */
    private static final Pattern YOUTUBE_URL = Pattern.compile(
            "^(?:https?://)?(?:www\\.|m\\.)?"
            + "(?:youtube\\.com/(?:watch\\?|.*[?&]v=|shorts/|embed/)"
            + "|youtu\\.be/)",
            Pattern.CASE_INSENSITIVE);

    /** Open Graph title, mirroring the reference extractor. */
    private static final Pattern OG_TITLE =
            Pattern.compile("<meta\\s+property=\"og:title\"\\s+content=\"(.*?)\"", Pattern.DOTALL);
    /** Fallback: the plain HTML &lt;title&gt; element. */
    private static final Pattern HTML_TITLE =
            Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    /**
     * Returns {@code true} if the URL points at a YouTube video, covering the
     * common forms: {@code youtube.com/watch}, {@code youtu.be/},
     * {@code m.youtube.com}, and {@code /shorts/} (plus {@code /embed/}).
     *
     * @param url the candidate URL; {@code null} is treated as non-matching.
     */
    public static boolean isYouTube(String url) {
        if (url == null) {
            return false;
        }
        return YOUTUBE_URL.matcher(url.trim()).find();
    }

    /**
     * Attempts to extract the video's title (best effort) and then delegates the
     * link to the operating system's default browser through
     * {@link Desktop#browse(URI)}.
     *
     * @param url the YouTube URL to open.
     * @return a {@link WebResponse#youTube(String, String, String)} describing
     *         the successful hand-off, or {@link WebResponse#error(String,
     *         String, int)} if the platform cannot launch a browser (headless
     *         environment, unsupported {@code Desktop}, or malformed URL).
     */
    public WebResponse openInNativeBrowser(String url) {
        String trimmed = url == null ? "" : url.trim();
        if (trimmed.isEmpty()) {
            return WebResponse.error(url, "No YouTube URL was provided.", 0);
        }

        // Best-effort title extraction — never fatal to the hand-off.
        String title = extractTitle(trimmed);

        // Guard the Desktop API: it is unavailable in headless setups and not
        // every platform supports the BROWSE action.
        if (!Desktop.isDesktopSupported()) {
            return WebResponse.error(trimmed,
                    "Cannot open YouTube: the java.awt.Desktop API is not "
                    + "supported in this environment (likely headless). "
                    + "Copy this link into your browser instead: " + trimmed,
                    0);
        }
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            return WebResponse.error(trimmed,
                    "Cannot open YouTube: launching a browser (Desktop.Action."
                    + "BROWSE) is not supported on this platform. "
                    + "Copy this link into your browser instead: " + trimmed,
                    0);
        }

        try {
            URI uri = new URI(trimmed);
            desktop.browse(uri);
        } catch (URISyntaxException e) {
            return WebResponse.error(trimmed,
                    "Cannot open YouTube: the link is not a valid URI ("
                    + e.getMessage() + ").", 0);
        } catch (IOException e) {
            return WebResponse.error(trimmed,
                    "Cannot open YouTube: the operating system failed to launch "
                    + "a browser (" + e.getMessage() + ").", 0);
        }

        String shown = (title == null || title.isEmpty()) ? trimmed : title;
        String message = "YouTube video handed off to your default browser: "
                + shown + ". The built-in viewer cannot play video (no "
                + "JavaScript or modern streaming), so the OS browser was used.";
        return WebResponse.youTube(trimmed, title, message);
    }

    /**
     * Fetches the page and returns the first available title, decoded of the
     * handful of escapes YouTube emits. Returns an empty string on any failure.
     */
    private String extractTitle(String videoUrl) {
        String html;
        try {
            html = fetchHtml(videoUrl);
        } catch (IOException e) {
            return "";
        }
        Matcher og = OG_TITLE.matcher(html);
        if (og.find()) {
            String value = og.group(1);
            if (value != null && !value.trim().isEmpty()) {
                return unescape(value.trim());
            }
        }
        Matcher title = HTML_TITLE.matcher(html);
        if (title.find()) {
            String value = title.group(1);
            if (value != null && !value.trim().isEmpty()) {
                return unescape(value.trim());
            }
        }
        return "";
    }

    /** Reads the raw HTML over HTTP(S) using a browser-like user-agent. */
    private String fetchHtml(String videoUrl) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL target = new URL(videoUrl);
            connection = (HttpURLConnection) target.openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");

            int status = connection.getResponseCode();
            if (status < 200 || status >= 400) {
                throw new IOException("Server returned HTTP " + status);
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

    /** Decodes the small set of HTML / unicode escapes seen in titles. */
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
        return result.replace("\t", " ").replace("\r", " ").replace("\n", " ").trim();
    }
}
