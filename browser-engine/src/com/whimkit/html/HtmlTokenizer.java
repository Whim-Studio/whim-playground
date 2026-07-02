package com.whimkit.html;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns an HTML source string into a flat list of {@link HtmlToken}s.
 *
 * <p>This is a pragmatic realization of the WHATWG tokenizer state machine.
 * Rather than a formal per-character state enum it scans the input with an
 * index cursor and a handful of focused sub-parsers ({@link #readStartTag},
 * {@link #readEndTag}, {@link #readComment}, ...), which is easier to follow
 * while still handling the constructs real pages use:</p>
 *
 * <ul>
 *   <li>DOCTYPE, comments ({@code <!-- ... -->}), bogus comments, start/end tags;</li>
 *   <li>attributes in double-quoted, single-quoted, unquoted, and value-less
 *       forms, plus the self-closing {@code /&gt;} flag;</li>
 *   <li>character references decoded via {@link HtmlEntities} in text, RCDATA,
 *       and attribute values (but <em>not</em> in raw-text);</li>
 *   <li>raw-text elements ({@code <script>}, {@code <style>}) and RCDATA
 *       elements ({@code <textarea>}, {@code <title>}) whose content is swallowed
 *       verbatim until the matching end tag.</li>
 * </ul>
 *
 * <p>The tokenizer never throws on malformed input: unterminated tags, comments,
 * and references degrade to sensible literal text. Adjacent character data is
 * coalesced into a single CHARACTER token. Instances are single-use and
 * single-threaded.</p>
 */
final class HtmlTokenizer {

    private final String in;
    private final int len;
    private int pos;

    /** Accumulates ordinary character data so it can be emitted as one token. */
    private final StringBuilder textBuf = new StringBuilder();

    private final List<HtmlToken> out = new ArrayList<HtmlToken>();

    HtmlTokenizer(String input) {
        this.in = input == null ? "" : input;
        this.len = this.in.length();
    }

    /**
     * Runs the machine to completion.
     *
     * @return the full token stream, terminated by a single EOF token.
     */
    List<HtmlToken> tokenize() {
        while (pos < len) {
            char c = in.charAt(pos);
            if (c == '<') {
                if (!tryConsumeMarkup()) {
                    // A '<' that does not begin markup is literal text.
                    textBuf.append('<');
                    pos++;
                }
            } else {
                textBuf.append(c);
                pos++;
            }
        }
        flushText();
        out.add(HtmlToken.eof());
        return out;
    }

    /**
     * Attempts to consume markup at the current '<'. Returns {@code false} (and
     * consumes nothing) if the '<' does not actually introduce a tag/comment,
     * so the caller can treat it as literal text.
     */
    private boolean tryConsumeMarkup() {
        int next = pos + 1;
        if (next >= len) return false;
        char c = in.charAt(next);

        if (c == '!') {
            readMarkupDeclaration();
            return true;
        }
        if (c == '/') {
            if (next + 1 < len && isAsciiLetter(in.charAt(next + 1))) {
                readEndTag();
                return true;
            }
            // "</" not followed by a name -> bogus comment.
            readBogusComment();
            return true;
        }
        if (c == '?') {
            readBogusComment();
            return true;
        }
        if (isAsciiLetter(c)) {
            readStartTag();
            return true;
        }
        return false;
    }

    // --- markup declarations: comments and DOCTYPE ------------------------

    private void readMarkupDeclaration() {
        // pos points at '<', pos+1 at '!'.
        if (in.startsWith("<!--", pos)) {
            readComment();
        } else if (regionMatchesIgnoreCase(pos + 2, "doctype")) {
            readDoctype();
        } else {
            readBogusComment();
        }
    }

    private void readComment() {
        flushText();
        int start = pos + 4; // after "<!--"
        int end = in.indexOf("-->", start);
        String data;
        if (end < 0) {
            data = in.substring(start);
            pos = len;
        } else {
            data = in.substring(start, end);
            pos = end + 3;
        }
        out.add(HtmlToken.comment(data));
    }

    /** Handles {@code <! ...>} and {@code <? ...>} recovery as a comment. */
    private void readBogusComment() {
        flushText();
        int start = pos + 1; // skip '<'; keep '!'/'?'/'/' out below
        // Skip the introducer char ('!' , '?' or '/').
        if (start < len) start++;
        int end = in.indexOf('>', start);
        String data;
        if (end < 0) {
            data = in.substring(start);
            pos = len;
        } else {
            data = in.substring(start, end);
            pos = end + 1;
        }
        out.add(HtmlToken.comment(data));
    }

    private void readDoctype() {
        flushText();
        int start = pos + 2; // after "<!"
        int end = in.indexOf('>', start);
        String body;
        if (end < 0) {
            body = in.substring(start);
            pos = len;
        } else {
            body = in.substring(start, end);
            pos = end + 1;
        }
        // body looks like "doctype html ..." — extract the root name if present.
        String name = "";
        String trimmed = body.trim();
        int sp = indexOfWhitespace(trimmed);
        if (sp >= 0) {
            String rest = trimmed.substring(sp).trim();
            int sp2 = indexOfWhitespace(rest);
            name = (sp2 < 0 ? rest : rest.substring(0, sp2));
        }
        out.add(HtmlToken.doctype(name.toLowerCase(Locale.ROOT)));
    }

    // --- tags -------------------------------------------------------------

    private void readEndTag() {
        flushText();
        pos += 2; // skip "</"
        int nameStart = pos;
        while (pos < len && isTagNameChar(in.charAt(pos))) pos++;
        String name = in.substring(nameStart, pos).toLowerCase(Locale.ROOT);
        // Skip anything up to and including '>'.
        skipToTagEnd();
        out.add(HtmlToken.endTag(name));
    }

    private void readStartTag() {
        flushText();
        pos++; // skip '<'
        int nameStart = pos;
        while (pos < len && isTagNameChar(in.charAt(pos))) pos++;
        String name = in.substring(nameStart, pos).toLowerCase(Locale.ROOT);
        HtmlToken tok = HtmlToken.startTag(name);

        readAttributes(tok);
        out.add(tok);

        // Enter raw-text / RCDATA scanning for elements whose content is opaque.
        if (!tok.selfClosing) {
            if (isRawText(name)) {
                consumeRawText(name, false);
            } else if (isRcData(name)) {
                consumeRawText(name, true);
            }
        }
    }

    /** Parses attributes and the closing '>' (or self-closing '/&gt;'). */
    private void readAttributes(HtmlToken tok) {
        while (pos < len) {
            skipWhitespace();
            if (pos >= len) return;
            char c = in.charAt(pos);
            if (c == '>') {
                pos++;
                return;
            }
            if (c == '/') {
                // Possible self-closing marker.
                if (pos + 1 < len && in.charAt(pos + 1) == '>') {
                    tok.selfClosing = true;
                    pos += 2;
                    return;
                }
                pos++; // stray slash; ignore
                continue;
            }
            // Attribute name.
            int nameStart = pos;
            while (pos < len) {
                char d = in.charAt(pos);
                if (isWhitespace(d) || d == '=' || d == '>' || d == '/') break;
                pos++;
            }
            String attrName = in.substring(nameStart, pos).toLowerCase(Locale.ROOT);
            if (attrName.isEmpty()) {
                // No progress possible on this char; skip it defensively.
                pos++;
                continue;
            }
            skipWhitespace();
            String attrValue = "";
            if (pos < len && in.charAt(pos) == '=') {
                pos++; // skip '='
                skipWhitespace();
                attrValue = readAttributeValue();
            }
            tok.addAttribute(attrName, attrValue);
        }
    }

    private String readAttributeValue() {
        if (pos >= len) return "";
        char q = in.charAt(pos);
        if (q == '"' || q == '\'') {
            pos++; // skip opening quote
            int start = pos;
            while (pos < len && in.charAt(pos) != q) pos++;
            String raw = in.substring(start, pos);
            if (pos < len) pos++; // skip closing quote
            return HtmlEntities.decode(raw);
        }
        // Unquoted value: up to whitespace or '>'.
        int start = pos;
        while (pos < len) {
            char c = in.charAt(pos);
            if (isWhitespace(c) || c == '>') break;
            pos++;
        }
        return HtmlEntities.decode(in.substring(start, pos));
    }

    /**
     * Consumes the opaque body of a raw-text ({@code decode == false}) or RCDATA
     * ({@code decode == true}) element, up to its matching end tag, then emits a
     * CHARACTER token for the body and an END_TAG token.
     */
    private void consumeRawText(String tagName, boolean decode) {
        String closer = "</" + tagName;
        int idx = indexOfIgnoreCase(closer, pos);
        String content;
        if (idx < 0) {
            content = in.substring(pos);
            pos = len;
        } else {
            content = in.substring(pos, idx);
            pos = idx;
        }
        if (!content.isEmpty()) {
            out.add(HtmlToken.character(decode ? HtmlEntities.decode(content) : content));
        }
        if (pos < len) {
            // Consume the "</tag ...>" end tag.
            pos += 2; // skip "</"
            while (pos < len && isTagNameChar(in.charAt(pos))) pos++;
            skipToTagEnd();
            out.add(HtmlToken.endTag(tagName));
        }
    }

    // --- helpers ----------------------------------------------------------

    private void flushText() {
        if (textBuf.length() == 0) return;
        out.add(HtmlToken.character(HtmlEntities.decode(textBuf.toString())));
        textBuf.setLength(0);
    }

    private void skipToTagEnd() {
        while (pos < len && in.charAt(pos) != '>') pos++;
        if (pos < len) pos++; // consume '>'
    }

    private void skipWhitespace() {
        while (pos < len && isWhitespace(in.charAt(pos))) pos++;
    }

    private int indexOfIgnoreCase(String needle, int from) {
        int n = needle.length();
        for (int i = from; i + n <= len; i++) {
            if (in.regionMatches(true, i, needle, 0, n)) return i;
        }
        return -1;
    }

    private boolean regionMatchesIgnoreCase(int at, String s) {
        return in.regionMatches(true, at, s, 0, s.length());
    }

    private static int indexOfWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (isWhitespace(s.charAt(i))) return i;
        }
        return -1;
    }

    private static boolean isAsciiLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isTagNameChar(char c) {
        return isAsciiLetter(c) || (c >= '0' && c <= '9') || c == '-' || c == ':' || c == '_';
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    private static boolean isRawText(String name) {
        return name.equals("script") || name.equals("style");
    }

    private static boolean isRcData(String name) {
        return name.equals("textarea") || name.equals("title");
    }
}
