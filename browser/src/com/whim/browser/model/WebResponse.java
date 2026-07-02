package com.whim.browser.model;

/**
 * Immutable value object describing the outcome of a single navigation request.
 *
 * <p>Thread-safety strategy: <b>immutability</b>. Every field is {@code final}
 * and is assigned exactly once in the private constructor. The class exposes no
 * mutators, so an instance can be shared freely across the networking threads
 * that produce it and the Swing Event Dispatch Thread that renders it without
 * any synchronization.</p>
 *
 * <p>Instances are created through the {@link #html}, {@link #youTube} and
 * {@link #error} factory methods rather than a public constructor so that each
 * {@link Kind} is always populated consistently.</p>
 */
public final class WebResponse {

    /** The category of a response, which selects how the view should render it. */
    public enum Kind {
        /** A successfully fetched HTML document. */
        HTML,
        /** A URL that should be handed off to the YouTube experience. */
        YOUTUBE_REDIRECT,
        /** A failed request; the body carries a human-readable message. */
        ERROR
    }

    private final String url;
    private final Kind kind;
    private final String contentType;
    private final String body;
    private final String title;
    private final int statusCode;

    private WebResponse(String url,
                        Kind kind,
                        String contentType,
                        String body,
                        String title,
                        int statusCode) {
        this.url = nullSafe(url);
        this.kind = kind;
        this.contentType = nullSafe(contentType);
        this.body = nullSafe(body);
        this.title = nullSafe(title);
        this.statusCode = statusCode;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    /**
     * Creates a response for a successfully fetched HTML document.
     *
     * @param url         the resolved URL that was fetched
     * @param contentType the reported MIME type (e.g. {@code text/html})
     * @param body        the HTML source text
     * @param title       the document title, or {@code ""} if unknown
     * @param statusCode  the HTTP status code (typically {@code 200})
     */
    public static WebResponse html(String url,
                                   String contentType,
                                   String body,
                                   String title,
                                   int statusCode) {
        return new WebResponse(url, Kind.HTML, contentType, body, title, statusCode);
    }

    /**
     * Creates a response signalling that the URL should be opened in the
     * dedicated YouTube experience rather than rendered as HTML.
     *
     * @param url     the YouTube URL
     * @param title   a display title, or {@code ""} if unknown
     * @param message a human-readable message describing the redirect
     */
    public static WebResponse youTube(String url, String title, String message) {
        return new WebResponse(url, Kind.YOUTUBE_REDIRECT, "", message, title, 0);
    }

    /**
     * Creates a response describing a failed request.
     *
     * @param url        the URL that was attempted
     * @param message    a human-readable error message (stored in the body)
     * @param statusCode the HTTP status code, or {@code 0} if the request never
     *                   reached the server
     */
    public static WebResponse error(String url, String message, int statusCode) {
        return new WebResponse(url, Kind.ERROR, "", message, "", statusCode);
    }

    /** @return the resolved URL this response is for (never {@code null}). */
    public String getUrl() {
        return url;
    }

    /** @return the {@link Kind} of this response. */
    public Kind getKind() {
        return kind;
    }

    /** @return the reported content type, or {@code ""} if not applicable. */
    public String getContentType() {
        return contentType;
    }

    /**
     * @return the HTML text for {@link Kind#HTML} responses, or a human-readable
     *         message for the other kinds (never {@code null}).
     */
    public String getBody() {
        return body;
    }

    /** @return the display title, or {@code ""} if unknown. */
    public String getTitle() {
        return title;
    }

    /** @return the HTTP status code, or {@code 0} when not applicable. */
    public int getStatusCode() {
        return statusCode;
    }

    /** @return {@code true} if this is an {@link Kind#HTML} response. */
    public boolean isHtml() {
        return kind == Kind.HTML;
    }

    /** @return {@code true} if this is a {@link Kind#YOUTUBE_REDIRECT} response. */
    public boolean isYouTube() {
        return kind == Kind.YOUTUBE_REDIRECT;
    }

    /** @return {@code true} if this is an {@link Kind#ERROR} response. */
    public boolean isError() {
        return kind == Kind.ERROR;
    }

    @Override
    public String toString() {
        return "WebResponse{" + kind + " " + statusCode + " " + url + "}";
    }
}
