package com.whimkit.net;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An immutable HTTP response handed from the networking worker thread to the
 * engine/EDT. Immutability (all fields {@code final}) makes the cross-thread
 * handoff safe without additional synchronization.
 */
public final class WebResponse {

    private final String finalUrl;      // after redirects
    private final int statusCode;
    private final Map<String, List<String>> headers;
    private final String contentType;   // lower-cased, params stripped
    private final Charset charset;
    private final byte[] body;
    private final String error;         // non-null only on transport failure

    public WebResponse(String finalUrl, int statusCode, Map<String, List<String>> headers,
                       String contentType, Charset charset, byte[] body, String error) {
        this.finalUrl = finalUrl;
        this.statusCode = statusCode;
        this.headers = headers == null ? Collections.<String, List<String>>emptyMap()
                : Collections.unmodifiableMap(headers);
        this.contentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        this.charset = charset == null ? Charset.forName("UTF-8") : charset;
        this.body = body == null ? new byte[0] : body;
        this.error = error;
    }

    public static WebResponse failure(String url, String error) {
        return new WebResponse(url, -1, null, "", null, new byte[0], error);
    }

    public String getFinalUrl() { return finalUrl; }
    public int getStatusCode() { return statusCode; }
    public Map<String, List<String>> getHeaders() { return headers; }
    public String getContentType() { return contentType; }
    public Charset getCharset() { return charset; }

    /** @return the raw response bytes (never {@code null}; empty on failure). */
    public byte[] getBody() { return body; }

    /** @return the response body decoded with the negotiated charset. */
    public String getText() {
        return new String(body, charset);
    }

    public boolean isOk() {
        return error == null && statusCode >= 200 && statusCode < 400;
    }

    public boolean isHtml() {
        return contentType.contains("text/html") || contentType.contains("application/xhtml");
    }

    public boolean isImage() {
        return contentType.startsWith("image/");
    }

    public String getError() { return error; }
}
