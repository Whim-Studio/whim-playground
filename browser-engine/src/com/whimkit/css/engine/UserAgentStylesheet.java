package com.whimkit.css.engine;

/**
 * The built-in User-Agent default stylesheet.
 *
 * <p>Every browser ships one of these: it gives bare HTML its familiar look
 * (block vs inline elements, heading sizes, list indentation, link colour) before
 * any author CSS applies. It is expressed as ordinary CSS text and parsed through
 * the same pipeline as author sheets, at the lowest cascade origin.</p>
 *
 * <p>Kept deliberately compact — the common structural and typographic tags, not
 * an exhaustive HTML rendering spec.</p>
 */
final class UserAgentStylesheet {

    private UserAgentStylesheet() { }

    /** @return the UA CSS source. */
    static String css() {
        return CSS;
    }

    private static final String CSS =
        // ---- block-level structure ----
        "html, body, div, p, h1, h2, h3, h4, h5, h6, ul, ol, li, dl, dt, dd,"
        + " blockquote, pre, form, fieldset, table, header, footer, nav, section,"
        + " article, aside, main, figure, figcaption, address, hr, details, summary"
        + " { display: block; }\n"
        + "head, script, style, title, meta, link, base { display: none; }\n"

        // ---- inline defaults ----
        + "span, a, b, strong, i, em, u, s, small, big, sub, sup, code, tt, kbd,"
        + " samp, cite, q, abbr, mark, label, font { display: inline; }\n"
        + "img, input, button, select, textarea { display: inline-block; }\n"

        // ---- body & typography ----
        + "body { margin: 8px; color: black; background-color: white;"
        + " font-family: serif; font-size: 16px; line-height: 1.2; }\n"

        // ---- headings ----
        + "h1 { font-size: 32px; font-weight: bold; margin-top: 21px; margin-bottom: 21px; }\n"
        + "h2 { font-size: 24px; font-weight: bold; margin-top: 20px; margin-bottom: 20px; }\n"
        + "h3 { font-size: 19px; font-weight: bold; margin-top: 18px; margin-bottom: 18px; }\n"
        + "h4 { font-size: 16px; font-weight: bold; margin-top: 21px; margin-bottom: 21px; }\n"
        + "h5 { font-size: 13px; font-weight: bold; margin-top: 22px; margin-bottom: 22px; }\n"
        + "h6 { font-size: 11px; font-weight: bold; margin-top: 24px; margin-bottom: 24px; }\n"

        // ---- paragraphs & blocks ----
        + "p { margin-top: 16px; margin-bottom: 16px; }\n"
        + "blockquote { margin-top: 16px; margin-bottom: 16px; margin-left: 40px; margin-right: 40px; }\n"
        + "figure { margin-top: 16px; margin-bottom: 16px; margin-left: 40px; margin-right: 40px; }\n"
        + "pre { font-family: monospace; white-space: pre; margin-top: 13px; margin-bottom: 13px; }\n"
        + "hr { margin-top: 8px; margin-bottom: 8px; border-top: 1px solid gray; }\n"
        + "address { font-style: italic; }\n"

        // ---- lists ----
        + "ul { margin-top: 16px; margin-bottom: 16px; padding-left: 40px; list-style-type: disc; }\n"
        + "ol { margin-top: 16px; margin-bottom: 16px; padding-left: 40px; list-style-type: decimal; }\n"
        + "li { display: list-item; }\n"
        + "ul ul, ol ol, ul ol, ol ul { margin-top: 0px; margin-bottom: 0px; }\n"
        + "dd { margin-left: 40px; }\n"

        // ---- inline text semantics ----
        + "a { color: blue; text-decoration: underline; }\n"
        + "b, strong { font-weight: bold; }\n"
        + "i, em, cite, var, dfn { font-style: italic; }\n"
        + "u, ins { text-decoration: underline; }\n"
        + "s, strike, del { text-decoration: line-through; }\n"
        + "small { font-size: 13px; }\n"
        + "big { font-size: 19px; }\n"
        + "code, kbd, samp, tt { font-family: monospace; }\n"
        + "mark { background-color: yellow; color: black; }\n"
        + "sub, sup { font-size: 11px; }\n"

        // ---- tables (approximate) ----
        + "table { display: table; }\n"
        + "caption { display: block; text-align: center; }\n"
        + "th { font-weight: bold; text-align: center; }\n"

        // ---- forms ----
        + "button, input, select, textarea { font-family: sans-serif; font-size: 13px; }\n";
}
