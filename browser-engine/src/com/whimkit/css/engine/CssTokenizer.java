package com.whimkit.css.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns raw CSS source into a small structural token stream consumed by
 * {@link CssParser}.
 *
 * <p>The tokenizer is deliberately structural rather than character-level: it
 * emits {@link CssToken.Type#SELECTOR selector preludes}, block delimiters,
 * whole {@link CssToken.Type#DECLARATION declarations}, and
 * {@link CssToken.Type#AT_KEYWORD at-rules}. The finer-grained parsing of
 * selector groups and {@code property:value} pairs happens later, from the raw
 * text each token preserves. This keeps the whole engine tolerant of malformed
 * input — a broken rule is dropped without derailing the stylesheet.</p>
 *
 * <h3>Robustness</h3>
 * <ul>
 *   <li>{@code /* ... *}{@code /} comments are stripped first.</li>
 *   <li>Brace, bracket, paren and string nesting are tracked so a {@code ';'} or
 *       {@code '}'} inside a {@code url(...)}, a quoted string, or an attribute
 *       selector does not prematurely end a declaration or block.</li>
 *   <li>At-rules are recognised: block at-rules ({@code @media}, {@code @font-face})
 *       have their whole block consumed and discarded; statement at-rules
 *       ({@code @import}, {@code @charset}) are consumed up to their {@code ';'}.
 *       Either way a single {@code AT_KEYWORD} token is emitted for visibility
 *       and the parser skips it.</li>
 * </ul>
 *
 * <p>Not thread-safe; construct one per parse.</p>
 */
final class CssTokenizer {

    private final String src;
    private final int len;
    private int pos;

    CssTokenizer(String css) {
        this.src = stripComments(css == null ? "" : css);
        this.len = src.length();
        this.pos = 0;
    }

    /** @return the full token stream, always terminated by an {@code EOF} token. */
    List<CssToken> tokenize() {
        List<CssToken> out = new ArrayList<CssToken>();
        while (pos < len) {
            skipWhitespace();
            if (pos >= len) break;
            char c = src.charAt(pos);
            if (c == '@') {
                readAtRule(out);
            } else if (c == '}') {
                // Stray close brace at top level: skip it.
                pos++;
            } else {
                readRule(out);
            }
        }
        out.add(new CssToken(CssToken.Type.EOF, ""));
        return out;
    }

    // --- rule bodies ------------------------------------------------------

    /** Reads a normal style rule: {@code selector-list '{' declarations '}'}. */
    private void readRule(List<CssToken> out) {
        int start = pos;
        int brace = indexOfTopLevel('{', pos);
        if (brace < 0) {
            // No block at all — malformed trailing text; consume the rest.
            pos = len;
            return;
        }
        String selector = src.substring(start, brace).trim();
        pos = brace + 1; // past '{'
        out.add(new CssToken(CssToken.Type.SELECTOR, selector));
        out.add(new CssToken(CssToken.Type.BLOCK_START, "{"));
        readDeclarations(out);
    }

    /** Reads declarations up to (and consuming) the matching {@code '}'}. */
    private void readDeclarations(List<CssToken> out) {
        StringBuilder decl = new StringBuilder();
        while (pos < len) {
            char c = src.charAt(pos);
            if (c == '"' || c == '\'') {
                int end = scanString(pos);
                decl.append(src, pos, end);
                pos = end;
                continue;
            }
            if (c == '(') {
                int end = scanParens(pos);
                decl.append(src, pos, end);
                pos = end;
                continue;
            }
            if (c == ';') {
                emitDeclaration(out, decl);
                decl.setLength(0);
                pos++;
                continue;
            }
            if (c == '}') {
                pos++;
                break;
            }
            if (c == '{') {
                // Nested block inside a declaration list (invalid) — skip it.
                pos = scanBraces(pos);
                continue;
            }
            decl.append(c);
            pos++;
        }
        emitDeclaration(out, decl);
        out.add(new CssToken(CssToken.Type.BLOCK_END, "}"));
    }

    private void emitDeclaration(List<CssToken> out, StringBuilder decl) {
        String d = decl.toString().trim();
        if (!d.isEmpty()) {
            out.add(new CssToken(CssToken.Type.DECLARATION, d));
        }
    }

    // --- at-rules ---------------------------------------------------------

    private void readAtRule(List<CssToken> out) {
        int start = pos;
        // Find whichever comes first at the top level: '{' or ';'.
        int brace = indexOfTopLevel('{', pos);
        int semi = indexOfTopLevel(';', pos);
        if (semi >= 0 && (brace < 0 || semi < brace)) {
            // Statement at-rule (@import, @charset, ...).
            String prelude = src.substring(start, semi).trim();
            out.add(new CssToken(CssToken.Type.AT_KEYWORD, prelude));
            pos = semi + 1;
            return;
        }
        if (brace >= 0) {
            // Block at-rule (@media, @font-face, @supports, ...): consume & discard.
            String prelude = src.substring(start, brace).trim();
            out.add(new CssToken(CssToken.Type.AT_KEYWORD, prelude));
            pos = scanBraces(brace);
            return;
        }
        // Unterminated at-rule: consume the rest.
        out.add(new CssToken(CssToken.Type.AT_KEYWORD, src.substring(start).trim()));
        pos = len;
    }

    // --- scanning helpers -------------------------------------------------

    /** @return index of the first top-level {@code target} at/after {@code from}, or -1. */
    private int indexOfTopLevel(char target, int from) {
        int i = from;
        while (i < len) {
            char c = src.charAt(i);
            if (c == '"' || c == '\'') {
                i = scanString(i);
            } else if (c == '(') {
                i = scanParens(i);
            } else if (c == target) {
                return i;
            } else {
                i++;
            }
        }
        return -1;
    }

    /** @param open index of a {@code '{'}; @return index just past its matching {@code '}'}. */
    private int scanBraces(int open) {
        int depth = 0;
        int i = open;
        while (i < len) {
            char c = src.charAt(i);
            if (c == '"' || c == '\'') {
                i = scanString(i);
            } else if (c == '(') {
                i = scanParens(i);
            } else if (c == '{') {
                depth++;
                i++;
            } else if (c == '}') {
                depth--;
                i++;
                if (depth == 0) return i;
            } else {
                i++;
            }
        }
        return len;
    }

    /** @param open index of a {@code '('}; @return index just past its matching {@code ')'}. */
    private int scanParens(int open) {
        int depth = 0;
        int i = open;
        while (i < len) {
            char c = src.charAt(i);
            if (c == '"' || c == '\'') {
                i = scanString(i);
            } else if (c == '(') {
                depth++;
                i++;
            } else if (c == ')') {
                depth--;
                i++;
                if (depth == 0) return i;
            } else {
                i++;
            }
        }
        return len;
    }

    /** @param start index of a quote char; @return index just past the closing quote. */
    private int scanString(int start) {
        char quote = src.charAt(start);
        int i = start + 1;
        while (i < len) {
            char c = src.charAt(i);
            if (c == '\\') {
                i += 2; // skip escaped char
                continue;
            }
            if (c == quote) return i + 1;
            i++;
        }
        return len;
    }

    private void skipWhitespace() {
        while (pos < len && Character.isWhitespace(src.charAt(pos))) pos++;
    }

    /** Removes {@code /* ... *}{@code /} comments, honoring string literals. */
    static String stripComments(String css) {
        int n = css.length();
        StringBuilder sb = new StringBuilder(n);
        int i = 0;
        while (i < n) {
            char c = css.charAt(i);
            if (c == '/' && i + 1 < n && css.charAt(i + 1) == '*') {
                int end = css.indexOf("*/", i + 2);
                if (end < 0) break; // unterminated comment: drop the rest
                i = end + 2;
            } else if (c == '"' || c == '\'') {
                int j = i + 1;
                while (j < n) {
                    char d = css.charAt(j);
                    if (d == '\\') { j += 2; continue; }
                    if (d == c) { j++; break; }
                    j++;
                }
                sb.append(css, i, Math.min(j, n));
                i = j;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }
}
