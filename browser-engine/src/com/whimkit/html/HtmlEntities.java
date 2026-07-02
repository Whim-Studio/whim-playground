package com.whimkit.html;

import java.util.HashMap;
import java.util.Map;

/**
 * Character-reference (entity) decoding for the HTML tokenizer.
 *
 * <p>This is a deliberately small, pragmatic subset of the WHATWG named-character
 * reference table plus full numeric decoding. It covers the references that
 * appear in the overwhelming majority of real documents: the five XML built-ins
 * (<code>&amp;amp; &amp;lt; &amp;gt; &amp;quot; &amp;apos;</code>), the common
 * typographic set (dashes, ellipsis, curly quotes), and numeric references in
 * both decimal (<code>&amp;#160;</code>) and hexadecimal
 * (<code>&amp;#xA0;</code>) forms.</p>
 *
 * <p>Decoding is intentionally lenient: an ampersand that does not begin a
 * recognized reference is emitted literally rather than treated as an error, and
 * the trailing semicolon is accepted-but-optional. This matches how browsers
 * recover from malformed markup and keeps the tokenizer from ever throwing.</p>
 *
 * <p>All methods are pure and stateless; the shared lookup map is immutable after
 * class initialization, so this type is safe to use from the single DOM-owning
 * thread without further synchronization.</p>
 */
final class HtmlEntities {

    /** Named references this engine understands, mapped to their replacement text. */
    private static final Map<String, String> NAMED = new HashMap<String, String>();

    static {
        NAMED.put("amp", "&");
        NAMED.put("lt", "<");
        NAMED.put("gt", ">");
        NAMED.put("quot", "\"");
        NAMED.put("apos", "'");
        NAMED.put("nbsp", " ");
        NAMED.put("copy", "©");
        NAMED.put("reg", "®");
        NAMED.put("trade", "™");
        NAMED.put("mdash", "—");
        NAMED.put("ndash", "–");
        NAMED.put("hellip", "…");
        NAMED.put("ldquo", "“");
        NAMED.put("rdquo", "”");
        NAMED.put("lsquo", "‘");
        NAMED.put("rsquo", "’");
    }

    private HtmlEntities() { }

    /**
     * Decodes every recognized character reference in {@code s}.
     *
     * @param s raw text possibly containing references; may be {@code null}.
     * @return the decoded text, or {@code s} unchanged when it contains no
     *         ampersand (the common fast path).
     */
    static String decode(String s) {
        if (s == null || s.indexOf('&') < 0) return s;
        int len = s.length();
        StringBuilder out = new StringBuilder(len);
        int i = 0;
        while (i < len) {
            char c = s.charAt(i);
            if (c != '&') {
                out.append(c);
                i++;
                continue;
            }
            // Attempt to parse a reference starting at i.
            int consumed = decodeOne(s, i, out);
            if (consumed > 0) {
                i += consumed;
            } else {
                out.append('&');
                i++;
            }
        }
        return out.toString();
    }

    /**
     * Tries to decode a single reference at {@code start} (which must point at
     * '&'). On success appends the replacement to {@code out} and returns the
     * number of source characters consumed (including the '&'); returns 0 when
     * the text at {@code start} is not a recognized reference.
     */
    private static int decodeOne(String s, int start, StringBuilder out) {
        int len = s.length();
        int i = start + 1; // char after '&'
        if (i >= len) return 0;

        if (s.charAt(i) == '#') {
            // Numeric reference: &#DDD; or &#xHHH;
            int j = i + 1;
            boolean hex = false;
            if (j < len && (s.charAt(j) == 'x' || s.charAt(j) == 'X')) {
                hex = true;
                j++;
            }
            int digitsStart = j;
            while (j < len && isRefDigit(s.charAt(j), hex)) j++;
            if (j == digitsStart) return 0; // no digits -> not a reference
            String num = s.substring(digitsStart, j);
            int cp;
            try {
                cp = Integer.parseInt(num, hex ? 16 : 10);
            } catch (NumberFormatException ex) {
                return 0; // overflow / garbage -> leave literal
            }
            appendCodePoint(out, cp);
            if (j < len && s.charAt(j) == ';') j++; // consume optional terminator
            return j - start;
        }

        // Named reference: letters/digits following '&'.
        int j = i;
        while (j < len && isNameChar(s.charAt(j))) j++;
        if (j == i) return 0;
        String name = s.substring(i, j);
        String rep = NAMED.get(name);
        if (rep == null) return 0; // unknown name -> emit '&' literally
        out.append(rep);
        if (j < len && s.charAt(j) == ';') j++;
        return j - start;
    }

    private static void appendCodePoint(StringBuilder out, int cp) {
        // Guard against invalid scalar values; fall back to the replacement char.
        if (cp <= 0 || cp > 0x10FFFF || (cp >= 0xD800 && cp <= 0xDFFF)) {
            out.append('�');
            return;
        }
        out.appendCodePoint(cp);
    }

    private static boolean isRefDigit(char c, boolean hex) {
        if (c >= '0' && c <= '9') return true;
        if (!hex) return false;
        return (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static boolean isNameChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
    }
}
