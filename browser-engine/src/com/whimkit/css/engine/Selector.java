package com.whimkit.css.engine;

import com.whimkit.dom.Element;
import com.whimkit.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A parsed CSS selector: a chain of compound {@link Simple} selectors joined by
 * descendant ({@code " "}) or child ({@code ">"}) combinators.
 *
 * <p>Supported pieces per compound selector: type ({@code div}), universal
 * ({@code *}), class ({@code .c}), id ({@code #i}) and attribute presence /
 * equality ({@code [attr]}, {@code [attr="v"]}). Anything the parser cannot make
 * sense of yields {@code null} from {@link #parse} and is dropped by the caller,
 * per the "ignore what you cannot parse" contract.</p>
 *
 * <h3>Specificity</h3>
 * <p>Computed as the classic {@code (id, class/attr, type)} triple and packed
 * into a single sortable int ({@code id*10000 + (class|attr)*100 + type}), which
 * is ample for realistic stylesheets.</p>
 *
 * <p>Matching assumes a single-threaded DOM (no concurrent mutation).</p>
 */
final class Selector {

    /** Combinator that precedes a compound selector in the chain. */
    enum Combinator { DESCENDANT, CHILD }

    /** An attribute condition: presence ({@code [a]}) or exact equality ({@code [a="v"]}). */
    static final class AttrCond {
        final String name;
        final String value; // null == presence only
        AttrCond(String name, String value) {
            this.name = name.toLowerCase(Locale.ROOT);
            this.value = value;
        }
        boolean matches(Element e) {
            if (!e.hasAttribute(name)) return false;
            if (value == null) return true;
            String actual = e.getAttribute(name);
            return value.equals(actual);
        }
    }

    /** A compound selector: an optional type plus any number of id/class/attr conditions. */
    static final class Simple {
        String type;                    // null or "*" == any element
        String id;                      // null == unconstrained
        final List<String> classes = new ArrayList<String>();
        final List<AttrCond> attrs = new ArrayList<AttrCond>();

        boolean matches(Element e) {
            if (type != null && !type.equals("*")
                    && !type.equals(e.getTagName())) return false;
            if (id != null && !id.equals(e.getId())) return false;
            if (!classes.isEmpty()) {
                List<String> have = e.getClassList();
                for (String c : classes) {
                    if (!have.contains(c)) return false;
                }
            }
            for (AttrCond a : attrs) {
                if (!a.matches(e)) return false;
            }
            return true;
        }
    }

    /** The compound selectors, left-to-right (index 0 is the leftmost/ancestor-most). */
    private final List<Simple> parts;
    /** Combinator[i] links parts[i-1] to parts[i]; combinators[0] is unused/null. */
    private final List<Combinator> combinators;

    private Selector(List<Simple> parts, List<Combinator> combinators) {
        this.parts = parts;
        this.combinators = combinators;
    }

    // --- matching ---------------------------------------------------------

    /**
     * @return {@code true} if this selector matches {@code target}, evaluated
     *         right-to-left walking up ancestors for combinators.
     */
    boolean matches(Element target) {
        int last = parts.size() - 1;
        if (last < 0) return false;
        if (!parts.get(last).matches(target)) return false;
        return matchFrom(last - 1, target);
    }

    /** Recursively satisfies parts[0..idx] against ancestors of {@code subject}. */
    private boolean matchFrom(int idx, Element subject) {
        if (idx < 0) return true;
        Combinator comb = combinators.get(idx + 1);
        Simple part = parts.get(idx);
        if (comb == Combinator.CHILD) {
            Element parent = parentElement(subject);
            return parent != null && part.matches(parent) && matchFrom(idx - 1, parent);
        }
        // DESCENDANT: try every ancestor.
        Element anc = parentElement(subject);
        while (anc != null) {
            if (part.matches(anc) && matchFrom(idx - 1, anc)) return true;
            anc = parentElement(anc);
        }
        return false;
    }

    private static Element parentElement(Element e) {
        Node p = e.getParentNode();
        return (p instanceof Element) ? (Element) p : null;
    }

    // --- specificity ------------------------------------------------------

    /** @return specificity packed as {@code id*10000 + classAttr*100 + type}. */
    int specificity() {
        int ids = 0, classAttr = 0, types = 0;
        for (Simple s : parts) {
            if (s.id != null) ids++;
            classAttr += s.classes.size() + s.attrs.size();
            if (s.type != null && !s.type.equals("*")) types++;
        }
        return ids * 10000 + classAttr * 100 + types;
    }

    // --- parsing ----------------------------------------------------------

    /**
     * Parses one complex selector (no commas — the caller splits groups first).
     *
     * @return the selector, or {@code null} if it is empty or malformed.
     */
    static Selector parse(String text) {
        if (text == null) return null;
        String s = text.trim();
        if (s.isEmpty()) return null;
        int n = s.length();

        List<Simple> parts = new ArrayList<Simple>();
        List<Combinator> combos = new ArrayList<Combinator>();

        int i = 0;
        // Skip any leading combinator noise.
        while (i < n && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == '>')) i++;
        Combinator comb = null; // combinator preceding the next token (null for the first)
        while (i < n) {
            int end = scanCompound(s, i, n);
            Simple simple = parseSimple(s.substring(i, end));
            if (simple == null) return null; // unparsable compound -> drop whole selector
            parts.add(simple);
            combos.add(comb);
            i = end;

            // Consume the combinator run between this token and the next.
            boolean child = false, sawAny = false;
            while (i < n && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == '>')) {
                if (s.charAt(i) == '>') child = true;
                sawAny = true;
                i++;
            }
            comb = child ? Combinator.CHILD : Combinator.DESCENDANT;
            if (!sawAny && i < n) return null; // adjacent tokens with no combinator (unsupported)
        }
        return parts.isEmpty() ? null : new Selector(parts, combos);
    }

    /** @return end index of the compound token starting at {@code i} (brackets may hold spaces). */
    private static int scanCompound(String s, int i, int n) {
        while (i < n) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c) || c == '>') break;
            if (c == '[') {
                int close = s.indexOf(']', i);
                i = (close < 0) ? n : close + 1;
            } else {
                i++;
            }
        }
        return i;
    }

    /** Parses a single compound selector like {@code a.b#c[href]}. */
    private static Simple parseSimple(String token) {
        Simple out = new Simple();
        int i = 0;
        int n = token.length();
        boolean any = false;
        while (i < n) {
            char c = token.charAt(i);
            if (c == '*') {
                out.type = "*";
                any = true;
                i++;
            } else if (c == '#') {
                int j = readIdent(token, i + 1);
                if (j == i + 1) return null;
                out.id = token.substring(i + 1, j);
                any = true;
                i = j;
            } else if (c == '.') {
                int j = readIdent(token, i + 1);
                if (j == i + 1) return null;
                out.classes.add(token.substring(i + 1, j));
                any = true;
                i = j;
            } else if (c == '[') {
                int end = token.indexOf(']', i);
                if (end < 0) return null;
                AttrCond cond = parseAttr(token.substring(i + 1, end));
                if (cond == null) return null;
                out.attrs.add(cond);
                any = true;
                i = end + 1;
            } else if (isIdentStart(c)) {
                int j = readIdent(token, i);
                out.type = token.substring(i, j).toLowerCase(Locale.ROOT);
                any = true;
                i = j;
            } else {
                // Unsupported piece (pseudo-class/element, combinators like +/~, etc.)
                return null;
            }
        }
        return any ? out : null;
    }

    /** Parses the inside of an attribute selector, e.g. {@code href} or {@code type="text"}. */
    private static AttrCond parseAttr(String body) {
        String b = body.trim();
        if (b.isEmpty()) return null;
        int eq = b.indexOf('=');
        if (eq < 0) {
            return new AttrCond(b, null);
        }
        String name = b.substring(0, eq).trim();
        // Only exact-match [attr="v"] is supported; ~= |= ^= $= *= are not.
        if (name.endsWith("~") || name.endsWith("|") || name.endsWith("^")
                || name.endsWith("$") || name.endsWith("*")) {
            return null;
        }
        String val = b.substring(eq + 1).trim();
        val = unquote(val);
        if (name.isEmpty()) return null;
        return new AttrCond(name, val);
    }

    private static String unquote(String v) {
        if (v.length() >= 2) {
            char a = v.charAt(0), z = v.charAt(v.length() - 1);
            if ((a == '"' || a == '\'') && a == z) {
                return v.substring(1, v.length() - 1);
            }
        }
        return v;
    }

    private static int readIdent(String s, int start) {
        int i = start;
        while (i < s.length() && isIdentChar(s.charAt(i))) i++;
        return i;
    }

    private static boolean isIdentStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '-';
    }

    private static boolean isIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-';
    }
}
