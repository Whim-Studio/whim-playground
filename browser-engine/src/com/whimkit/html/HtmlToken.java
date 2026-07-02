package com.whimkit.html;

import java.util.ArrayList;
import java.util.List;

/**
 * A single lexical token emitted by {@link HtmlTokenizer} and consumed by
 * {@link HtmlTreeBuilder}.
 *
 * <p>One flat class models every token kind (distinguished by {@link #type})
 * rather than a class hierarchy, which keeps the tokenizer→builder handoff a
 * simple {@code switch}. Only the fields relevant to a given {@link Type} are
 * populated; the rest stay at their defaults.</p>
 *
 * <p>Tokens are short-lived, single-threaded value objects — created, consumed,
 * and discarded on the parser thread. They are not shared across threads.</p>
 */
final class HtmlToken {

    /** The kinds of token the tokenizer produces. */
    enum Type { DOCTYPE, START_TAG, END_TAG, COMMENT, CHARACTER, EOF }

    /** A single attribute on a start tag: a lower-cased name and its value. */
    static final class Attr {
        final String name;
        final String value;
        Attr(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    final Type type;

    /** Tag name (lower-cased) for START_TAG/END_TAG, or the DOCTYPE name. */
    String name;

    /** Text payload for CHARACTER and COMMENT tokens. */
    String data;

    /** Whether a start tag carried the XML-style {@code /&gt;} self-closing flag. */
    boolean selfClosing;

    /** Ordered attribute list for START_TAG tokens; {@code null} otherwise. */
    List<Attr> attributes;

    private HtmlToken(Type type) {
        this.type = type;
    }

    static HtmlToken startTag(String name) {
        HtmlToken t = new HtmlToken(Type.START_TAG);
        t.name = name;
        t.attributes = new ArrayList<Attr>(4);
        return t;
    }

    static HtmlToken endTag(String name) {
        HtmlToken t = new HtmlToken(Type.END_TAG);
        t.name = name;
        return t;
    }

    static HtmlToken character(String data) {
        HtmlToken t = new HtmlToken(Type.CHARACTER);
        t.data = data;
        return t;
    }

    static HtmlToken comment(String data) {
        HtmlToken t = new HtmlToken(Type.COMMENT);
        t.data = data;
        return t;
    }

    static HtmlToken doctype(String name) {
        HtmlToken t = new HtmlToken(Type.DOCTYPE);
        t.name = name;
        return t;
    }

    static HtmlToken eof() {
        return new HtmlToken(Type.EOF);
    }

    void addAttribute(String attrName, String attrValue) {
        if (attrName == null || attrName.isEmpty()) return;
        // Per the spec, the first declaration of a duplicate attribute wins.
        for (Attr a : attributes) {
            if (a.name.equals(attrName)) return;
        }
        attributes.add(new Attr(attrName, attrValue == null ? "" : attrValue));
    }
}
