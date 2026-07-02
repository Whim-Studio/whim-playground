package com.whimkit.net.http;

import com.whimkit.net.ResourceLoader;
import com.whimkit.net.Url;
import com.whimkit.net.WebResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * The concrete networking subsystem: fetches resources over HTTP/HTTPS (plus the
 * {@code file:}, {@code data:} and {@code about:} pseudo-schemes) and returns an
 * immutable {@link WebResponse}. This is the implementation named by
 * {@link ResourceLoader}.
 *
 * <p><strong>What it does.</strong></p>
 * <ul>
 *   <li>Uses {@link HttpURLConnection}/{@code HttpsURLConnection} with browser-like
 *       request headers ({@code User-Agent}, {@code Accept}, {@code Accept-Language},
 *       {@code Accept-Encoding: gzip, deflate}).</li>
 *   <li>Follows redirects <em>manually</em> (up to {@value #MAX_REDIRECTS}) with
 *       {@code setInstanceFollowRedirects(false)}, resolving each {@code Location}
 *       against the current URL and recording the final URL in the response.
 *       Cookies are carried across hops by the process-wide {@link CookieManager}.</li>
 *   <li>Transparently decodes {@code gzip} and {@code deflate} response bodies.</li>
 *   <li>Negotiates the charset: {@code Content-Type} charset parameter, else a
 *       {@code <meta charset>} sniff of the first ~2&nbsp;KiB for HTML, else UTF-8.</li>
 *   <li>Serves and populates a bounded {@link ResourceCache}, skipping responses
 *       marked {@code Cache-Control: no-store}.</li>
 *   <li>Never throws to callers: every transport/timeout/TLS error becomes
 *       {@link WebResponse#failure(String, String)}.</li>
 * </ul>
 *
 * <p><strong>Thread-safety.</strong> This class is stateless apart from the shared
 * {@link ResourceCache} (internally synchronized) and immutable configuration, so
 * a single instance may be called concurrently from many worker threads. Each
 * {@link #load(String)} call uses only local {@link URLConnection} objects.</p>
 */
public final class HttpResourceLoader implements ResourceLoader {

    /** Maximum number of redirects followed before giving up. */
    public static final int MAX_REDIRECTS = 20;

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 15_000;

    private static final String USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) WhimKit/1.0 (Java; like Gecko) WhimKit/1.0";
    private static final String ACCEPT =
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/*,*/*;q=0.8";
    private static final String ACCEPT_LANGUAGE = "en-US,en;q=0.9";
    private static final String ACCEPT_ENCODING = "gzip, deflate";

    /** How many leading bytes of an HTML body to scan for a {@code <meta charset>}. */
    private static final int SNIFF_LIMIT = 2048;

    /** Pattern for the {@code charset} parameter of a Content-Type header. */
    private static final Pattern CHARSET_PARAM =
            Pattern.compile("charset\\s*=\\s*\"?([A-Za-z0-9_.:+-]+)\"?", Pattern.CASE_INSENSITIVE);

    /** Pattern for a {@code <meta charset=...>} or {@code http-equiv} content-type meta. */
    private static final Pattern META_CHARSET = Pattern.compile(
            "<meta[^>]+charset\\s*=\\s*[\"']?\\s*([A-Za-z0-9_.:+-]+)", Pattern.CASE_INSENSITIVE);

    /**
     * Installs a process-wide {@link CookieManager} as the default
     * {@link CookieHandler} exactly once, guarding against replacing a handler an
     * embedding application may have already installed.
     */
    static {
        installDefaultCookieManager();
    }

    private final ResourceCache cache;

    /** Creates a loader backed by a default-sized {@link ResourceCache}. */
    public HttpResourceLoader() {
        this(new ResourceCache());
    }

    /**
     * Creates a loader backed by the supplied cache.
     *
     * @param cache the response cache to use; must not be {@code null}.
     */
    public HttpResourceLoader(ResourceCache cache) {
        if (cache == null) throw new IllegalArgumentException("cache must not be null");
        this.cache = cache;
    }

    /** @return the cache this loader consults and populates. */
    public ResourceCache getCache() {
        return cache;
    }

    /**
     * Installs the shared cookie jar. Idempotent and safe to call from static
     * initialization on any thread; if a handler is already present it is left in
     * place.
     */
    public static synchronized void installDefaultCookieManager() {
        if (CookieHandler.getDefault() == null) {
            CookieManager manager = new CookieManager();
            manager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
            CookieHandler.setDefault(manager);
        }
    }

    @Override
    public WebResponse load(String url) {
        if (url == null || url.trim().isEmpty()) {
            return WebResponse.failure(String.valueOf(url), "empty URL");
        }
        url = url.trim();
        String scheme = schemeOf(url);
        try {
            if ("data".equals(scheme)) {
                return loadData(url);
            }
            if ("about".equals(scheme)) {
                return loadAbout(url);
            }
            if ("file".equals(scheme)) {
                return loadFile(url);
            }
            if ("http".equals(scheme) || "https".equals(scheme)) {
                return loadHttp(url);
            }
            return WebResponse.failure(url, "unsupported scheme: " + scheme);
        } catch (Throwable t) {
            // Absolute belt-and-braces: no exception ever escapes to the caller.
            return WebResponse.failure(url, describe(t));
        }
    }

    // ------------------------------------------------------------------ HTTP(S)

    private WebResponse loadHttp(String requestUrl) {
        WebResponse cached = cache.get(requestUrl);
        if (cached != null) {
            return cached;
        }

        String current = requestUrl;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            HttpURLConnection conn = null;
            try {
                URL u = new URL(current);
                URLConnection raw = u.openConnection();
                if (!(raw instanceof HttpURLConnection)) {
                    return WebResponse.failure(requestUrl, "not an HTTP connection: " + current);
                }
                conn = (HttpURLConnection) raw;
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setRequestProperty("Accept", ACCEPT);
                conn.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
                conn.setRequestProperty("Accept-Encoding", ACCEPT_ENCODING);

                int status = conn.getResponseCode();

                if (isRedirect(status)) {
                    String location = conn.getHeaderField("Location");
                    if (location == null || location.trim().isEmpty()) {
                        // Redirect status with no target — treat as a terminal response.
                        return finish(requestUrl, current, conn, status);
                    }
                    if (hop == MAX_REDIRECTS) {
                        return WebResponse.failure(requestUrl,
                                "too many redirects (>" + MAX_REDIRECTS + ")");
                    }
                    String next = Url.resolve(current, location.trim());
                    // Drain and disconnect before the next hop so keep-alive can reuse.
                    consumeQuietly(conn);
                    current = next;
                    continue;
                }

                return finish(requestUrl, current, conn, status);
            } catch (IOException e) {
                return WebResponse.failure(requestUrl, describe(e));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
        return WebResponse.failure(requestUrl, "too many redirects (>" + MAX_REDIRECTS + ")");
    }

    /** Builds the final response for a non-redirect status, reading and decoding the body. */
    private WebResponse finish(String requestUrl, String finalUrl,
                               HttpURLConnection conn, int status) throws IOException {
        Map<String, List<String>> headers = copyHeaders(conn.getHeaderFields());

        InputStream stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        byte[] raw = stream == null ? new byte[0] : readAll(stream);
        byte[] body = decodeBody(raw, firstHeader(headers, "Content-Encoding"));

        String rawContentType = conn.getContentType(); // may include charset param
        String contentType = stripParams(rawContentType);
        Charset charset = negotiateCharset(rawContentType, contentType, body);

        WebResponse response = new WebResponse(finalUrl, status, headers,
                contentType, charset, body, null);

        if (isCacheable(status, headers)) {
            cache.put(requestUrl, response);
        }
        return response;
    }

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303
                || status == 307 || status == 308;
    }

    /**
     * Best-effort cacheability: cache only successful responses that are not marked
     * {@code no-store} (or {@code no-cache}, treated conservatively as no-store here).
     */
    private static boolean isCacheable(int status, Map<String, List<String>> headers) {
        if (status < 200 || status >= 300) return false;
        String cc = firstHeader(headers, "Cache-Control");
        if (cc != null) {
            String lower = cc.toLowerCase(Locale.ROOT);
            if (lower.contains("no-store") || lower.contains("no-cache")) return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ file:

    private WebResponse loadFile(String url) {
        try {
            File file;
            try {
                file = new File(new URI(url));
            } catch (Exception uriEx) {
                // Fall back to a lenient parse of "file:///path" or "file:/path".
                String path = url.substring("file:".length());
                while (path.startsWith("/") && path.length() > 1 && path.charAt(1) == '/') {
                    path = path.substring(1);
                }
                file = new File(path);
            }
            if (!file.exists() || file.isDirectory()) {
                return WebResponse.failure(url, "file not found: " + file);
            }
            byte[] body = Files.readAllBytes(file.toPath());
            String contentType = guessFileContentType(file.getName());
            Charset charset = negotiateCharset(null, contentType, body);
            Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>();
            return new WebResponse(url, 200, headers, contentType, charset, body, null);
        } catch (IOException e) {
            return WebResponse.failure(url, describe(e));
        }
    }

    // ------------------------------------------------------------------ data:

    /**
     * Decodes an RFC 2397 {@code data:} URL:
     * {@code data:[<mediatype>][;base64],<data>}.
     */
    private WebResponse loadData(String url) {
        try {
            int comma = url.indexOf(',');
            if (comma < 0) {
                return WebResponse.failure(url, "malformed data: URL (no comma)");
            }
            String meta = url.substring("data:".length(), comma);
            String data = url.substring(comma + 1);

            boolean base64 = false;
            String contentType = "text/plain";
            String charsetName = "US-ASCII"; // RFC 2397 default
            if (!meta.isEmpty()) {
                String[] parts = meta.split(";");
                for (int i = 0; i < parts.length; i++) {
                    String p = parts[i].trim();
                    if (p.isEmpty()) continue;
                    if (p.equalsIgnoreCase("base64")) {
                        base64 = true;
                    } else if (p.toLowerCase(Locale.ROOT).startsWith("charset=")) {
                        charsetName = p.substring("charset=".length());
                    } else if (p.contains("/")) {
                        contentType = p.toLowerCase(Locale.ROOT);
                    }
                }
            }

            byte[] body;
            if (base64) {
                // Base64 payloads may be percent-encoded / whitespaced; normalize first.
                String clean = data.replaceAll("\\s+", "");
                body = Base64.getDecoder().decode(clean);
            } else {
                String decoded = URLDecoder.decode(data, "UTF-8");
                body = decoded.getBytes(safeCharset(charsetName, "UTF-8"));
            }
            Charset charset = safeCharset(charsetName, "UTF-8");
            Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>();
            return new WebResponse(url, 200, headers, stripParams(contentType), charset, body, null);
        } catch (Exception e) {
            return WebResponse.failure(url, describe(e));
        }
    }

    // ------------------------------------------------------------------ about:

    private WebResponse loadAbout(String url) {
        String rest = url.substring("about:".length()).toLowerCase(Locale.ROOT);
        String title;
        String message;
        if (rest.isEmpty() || rest.equals("blank")) {
            title = "";
            message = "";
        } else {
            title = "about:" + rest;
            message = "WhimKit has no built-in page for <code>about:" + escapeHtml(rest) + "</code>.";
        }
        String html = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>"
                + escapeHtml(title) + "</title></head><body>"
                + (message.isEmpty() ? "" : "<p>" + message + "</p>")
                + "</body></html>";
        byte[] body = html.getBytes(Charset.forName("UTF-8"));
        Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>();
        return new WebResponse(url, 200, headers, "text/html", Charset.forName("UTF-8"), body, null);
    }

    // ------------------------------------------------------------------ helpers

    /** Reads a stream fully into a byte array. */
    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
        byte[] buf = new byte[8192];
        int n;
        try {
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
        } finally {
            try { in.close(); } catch (IOException ignored) { }
        }
        return out.toByteArray();
    }

    /**
     * Decodes a body given its {@code Content-Encoding}. Supports {@code gzip} and
     * {@code deflate} (both zlib-wrapped and raw). Unknown encodings are returned
     * unchanged.
     */
    private static byte[] decodeBody(byte[] raw, String encoding) throws IOException {
        if (raw.length == 0 || encoding == null) return raw;
        String enc = encoding.trim().toLowerCase(Locale.ROOT);
        if (enc.contains("gzip")) {
            return readAll(new GZIPInputStream(new ByteArrayInputStream(raw)));
        }
        if (enc.contains("deflate")) {
            try {
                // Standard zlib-wrapped deflate.
                return readAll(new InflaterInputStream(new ByteArrayInputStream(raw)));
            } catch (IOException zlibFailed) {
                // Some servers send raw (headerless) deflate; retry with nowrap.
                return readAll(new InflaterInputStream(
                        new ByteArrayInputStream(raw), new Inflater(true)));
            }
        }
        return raw;
    }

    /**
     * Negotiates the charset: {@code Content-Type} charset parameter first, then a
     * {@code <meta charset>} sniff for HTML bodies, else UTF-8.
     */
    private static Charset negotiateCharset(String rawContentType, String contentType, byte[] body) {
        if (rawContentType != null) {
            Matcher m = CHARSET_PARAM.matcher(rawContentType);
            if (m.find()) {
                Charset c = tryCharset(m.group(1));
                if (c != null) return c;
            }
        }
        boolean html = contentType != null
                && (contentType.contains("html") || contentType.contains("xml"));
        if (html && body.length > 0) {
            int scanLen = Math.min(body.length, SNIFF_LIMIT);
            // Latin-1 preserves every byte so ASCII markup is readable regardless.
            String head = new String(body, 0, scanLen, Charset.forName("ISO-8859-1"));
            Matcher m = META_CHARSET.matcher(head);
            if (m.find()) {
                Charset c = tryCharset(m.group(1));
                if (c != null) return c;
            }
        }
        return Charset.forName("UTF-8");
    }

    private static Charset tryCharset(String name) {
        if (name == null) return null;
        String n = name.trim();
        if (n.isEmpty()) return null;
        try {
            return Charset.isSupported(n) ? Charset.forName(n) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Charset safeCharset(String name, String fallback) {
        Charset c = tryCharset(name);
        return c != null ? c : Charset.forName(fallback);
    }

    /** Strips {@code ;charset=...} and other parameters, lower-casing the media type. */
    private static String stripParams(String contentType) {
        if (contentType == null) return "";
        int semi = contentType.indexOf(';');
        String type = semi >= 0 ? contentType.substring(0, semi) : contentType;
        return type.trim().toLowerCase(Locale.ROOT);
    }

    private static String schemeOf(String url) {
        int colon = url.indexOf(':');
        if (colon <= 0) return "";
        return url.substring(0, colon).toLowerCase(Locale.ROOT);
    }

    /** Copies a header map into a fresh mutable {@link LinkedHashMap}, dropping the null status-line key. */
    private static Map<String, List<String>> copyHeaders(Map<String, List<String>> src) {
        Map<String, List<String>> out = new LinkedHashMap<String, List<String>>();
        if (src != null) {
            for (Map.Entry<String, List<String>> e : src.entrySet()) {
                if (e.getKey() == null) continue; // the HTTP status line
                out.put(e.getKey(), new ArrayList<String>(e.getValue()));
            }
        }
        return out;
    }

    /** Case-insensitive first-value lookup over a header map. */
    private static String firstHeader(Map<String, List<String>> headers, String name) {
        if (headers == null) return null;
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) {
                List<String> v = e.getValue();
                return (v != null && !v.isEmpty()) ? v.get(0) : null;
            }
        }
        return null;
    }

    private static void consumeQuietly(HttpURLConnection conn) {
        try {
            InputStream in = conn.getInputStream();
            if (in != null) readAll(in);
        } catch (IOException e) {
            InputStream err = conn.getErrorStream();
            if (err != null) {
                try { readAll(err); } catch (IOException ignored) { }
            }
        }
    }

    private static String guessFileContentType(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".html") || lower.endsWith(".htm") || lower.endsWith(".xhtml")) return "text/html";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".js") || lower.endsWith(".mjs")) return "application/javascript";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".xml")) return "application/xml";
        if (lower.endsWith(".txt") || lower.endsWith(".text")) return "text/plain";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".bmp")) return "image/bmp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String describe(Throwable t) {
        String msg = t.getMessage();
        return t.getClass().getSimpleName() + (msg != null ? ": " + msg : "");
    }
}
