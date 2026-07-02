package com.whim.browser.engine;

import com.whim.browser.model.WebResponse;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The network + parsing engine that backs the Swing browser.
 *
 * <p>All blocking I/O runs on a background {@link SwingWorker} thread; the two
 * {@link EngineCallback} hooks fire on the EDT so the UI can update safely.</p>
 *
 * <p><b>Why YouTube is special-cased.</b> The UI renders pages with
 * {@link javax.swing.JEditorPane}, which supports only legacy HTML&nbsp;3.2/CSS
 * and has no JavaScript engine or modern video pipeline. A YouTube watch page
 * therefore cannot function inside the viewer. Instead of rendering a broken
 * shell, the engine detects YouTube links via {@link YouTubeHandler} and
 * delegates them to the operating system's default browser, returning a
 * {@link WebResponse.Kind#YOUTUBE_REDIRECT} that the UI can explain to the
 * user. Everything else is fetched over {@link HttpURLConnection} and returned
 * as {@link WebResponse.Kind#HTML} for {@code JEditorPane} to render.</p>
 */
public final class BrowserEngine {

    /** Browser-like agent so sites serve their standard desktop markup. */
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 20000;

    /** Extracts the document title for the address bar / tab label. */
    private static final Pattern HTML_TITLE =
            Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private final YouTubeHandler youTubeHandler = new YouTubeHandler();

    /**
     * Loads {@code url} asynchronously and reports the result via
     * {@code callback}.
     *
     * <p>The URL is normalized first (a bare host such as {@code example.com}
     * gets an {@code http://} scheme). {@link EngineCallback#onStart(String)}
     * fires on the EDT before any network work; {@link
     * EngineCallback#onResult(WebResponse)} fires on the EDT when done.</p>
     *
     * @param url      the address to load; may be a bare host.
     * @param callback the UI hooks; must not be {@code null}.
     */
    public void load(final String url, final EngineCallback callback) {
        final String normalized = normalize(url);

        // Announce the start on the EDT before background work begins.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                callback.onStart(normalized);
            }
        });

        new SwingWorker<WebResponse, Void>() {
            @Override
            protected WebResponse doInBackground() {
                if (YouTubeHandler.isYouTube(normalized)) {
                    return youTubeHandler.openInNativeBrowser(normalized);
                }
                return fetch(normalized);
            }

            @Override
            protected void done() {
                WebResponse response;
                try {
                    response = get();
                } catch (Exception e) {
                    // Surface any unexpected failure (including cancellation)
                    // as an error response rather than swallowing it.
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    response = WebResponse.error(normalized,
                            "Unexpected error: " + cause.getMessage(), 0);
                }
                // done() already runs on the EDT.
                callback.onResult(response);
            }
        }.execute();
    }

    /**
     * Fetches a non-YouTube page and wraps it in a {@link WebResponse}. Any
     * {@link IOException} is converted into {@link WebResponse#error}.
     */
    private WebResponse fetch(String pageUrl) {
        HttpURLConnection connection = null;
        int status = 0;
        try {
            URL target = new URL(pageUrl);
            connection = (HttpURLConnection) target.openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            connection.setRequestProperty("Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

            status = connection.getResponseCode();
            if (status < 200 || status >= 400) {
                return WebResponse.error(pageUrl,
                        "Server returned HTTP " + status + " for " + pageUrl,
                        status);
            }

            String contentType = connection.getContentType();
            String charset = charsetFrom(contentType);
            String body = readBody(connection.getInputStream(), charset);
            String title = extractTitle(body);

            return WebResponse.html(pageUrl, contentType, body, title, status);
        } catch (IOException e) {
            return WebResponse.error(pageUrl,
                    "Could not load " + pageUrl + ": " + e.getMessage(), status);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /** Reads the response body fully as text in the given charset. */
    private static String readBody(InputStream stream, String charset) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, Charset.forName(charset)));
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
    }

    /** Extracts the &lt;title&gt; text, or an empty string if none is present. */
    private static String extractTitle(String html) {
        Matcher matcher = HTML_TITLE.matcher(html);
        if (matcher.find()) {
            String value = matcher.group(1);
            if (value != null) {
                return value.replace("\r", " ").replace("\n", " ").trim();
            }
        }
        return "";
    }

    /**
     * Derives the response charset from a {@code Content-Type} header, falling
     * back to UTF-8 when it is absent, unparseable, or unsupported.
     */
    private static String charsetFrom(String contentType) {
        if (contentType != null) {
            int idx = contentType.toLowerCase().indexOf("charset=");
            if (idx >= 0) {
                String cs = contentType.substring(idx + "charset=".length()).trim();
                int sep = cs.indexOf(';');
                if (sep >= 0) {
                    cs = cs.substring(0, sep).trim();
                }
                // Strip optional quotes.
                cs = cs.replace("\"", "").replace("'", "").trim();
                if (!cs.isEmpty() && Charset.isSupported(cs)) {
                    return cs;
                }
            }
        }
        return "UTF-8";
    }

    /**
     * Normalizes user input: trims whitespace and prepends {@code http://} when
     * no scheme is present so bare hosts like {@code example.com} still load.
     */
    private static String normalize(String url) {
        String trimmed = url == null ? "" : url.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        if (!trimmed.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) {
            return "http://" + trimmed;
        }
        return trimmed;
    }
}
