package com.whimkit.html;

import com.whimkit.dom.Document;

import java.util.List;

/**
 * Concrete {@link HtmlParser} for WhimKit: a two-stage, tolerant HTML5 parser
 * built on the Java 8 standard library only.
 *
 * <p>The pipeline mirrors the WHATWG model in two cooperating stages:</p>
 * <ol>
 *   <li>{@link HtmlTokenizer} scans the source into a flat token stream
 *       (DOCTYPE, start/end tags with attributes, comments, and character data),
 *       decoding character references and swallowing raw-text/RCDATA element
 *       bodies verbatim;</li>
 *   <li>{@link HtmlTreeBuilder} consumes those tokens against an open-element
 *       stack, applying implied-tag insertion, void-element and optional-end-tag
 *       rules, and misnest recovery to produce a foundation {@link Document}.</li>
 * </ol>
 *
 * <p>Per the {@link HtmlParser} contract this method is fully tolerant: it never
 * throws on malformed markup and always returns a non-{@code null} document with
 * at least {@code <html>}, {@code <head>}, and {@code <body>} present. The parser
 * is stateless and cheap to construct; each {@link #parse} call is independent.
 * The {@link Document} it returns is single-threaded by contract (see
 * {@code com.whimkit.dom.Node}).</p>
 */
public final class Html5Parser implements HtmlParser {

    @Override
    public Document parse(String html, String baseUri) {
        String source = html == null ? "" : html;
        try {
            List<HtmlToken> tokens = new HtmlTokenizer(source).tokenize();
            return new HtmlTreeBuilder().build(tokens, baseUri);
        } catch (RuntimeException ex) {
            // Absolute last-resort guard: the contract forbids throwing, so fall
            // back to an empty-but-valid document rather than propagating.
            return new HtmlTreeBuilder().build(
                    new HtmlTokenizer("").tokenize(), baseUri);
        }
    }
}
