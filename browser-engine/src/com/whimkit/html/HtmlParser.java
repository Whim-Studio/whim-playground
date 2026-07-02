package com.whimkit.html;

import com.whimkit.dom.Document;

/**
 * Contract for turning an HTML byte/character stream into a {@link Document}.
 *
 * <p>Implemented by {@code com.whimkit.html.Html5Parser}. The parser must be
 * tolerant of malformed markup (implied tags, unclosed elements, stray text)
 * and must never throw on bad input — it returns a best-effort tree.</p>
 */
public interface HtmlParser {

    /**
     * @param html    the decoded HTML source.
     * @param baseUri the document base URI (used to seed {@link Document#getBaseUri()}).
     * @return a fully built document tree (never {@code null}).
     */
    Document parse(String html, String baseUri);
}
