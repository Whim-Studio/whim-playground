package com.whimkit.css.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembles the {@link CssTokenizer} token stream into a flat list of
 * {@link StyleRule}s.
 *
 * <p>Each source rule's selector prelude is split on commas into individual
 * selectors; every selector that parses becomes its own {@code StyleRule}
 * sharing the block's declaration list. Selectors, declarations, or entire
 * rules that fail to parse are silently dropped so one bad rule never breaks the
 * sheet. At-rules are ignored (the tokenizer already discarded their bodies).</p>
 *
 * <p>The {@code baseOrder} lets the caller keep author sheets ordered after the
 * UA sheet; rules are numbered by their appearance so later rules win ties.</p>
 */
final class CssParser {

    private CssParser() { }

    /**
     * Parses a stylesheet source into rules.
     *
     * @param css       raw CSS text
     * @param baseOrder the starting order rank for the first rule
     * @return the parsed rules (possibly empty); never {@code null}
     */
    static List<StyleRule> parse(String css, int baseOrder) {
        List<StyleRule> rules = new ArrayList<StyleRule>();
        List<CssToken> tokens = new CssTokenizer(css).tokenize();
        int order = baseOrder;
        int i = 0;
        while (i < tokens.size()) {
            CssToken t = tokens.get(i);
            if (t.type == CssToken.Type.SELECTOR) {
                // Expect: SELECTOR BLOCK_START DECLARATION* BLOCK_END
                List<Declaration> decls = new ArrayList<Declaration>();
                int j = i + 1;
                if (j < tokens.size() && tokens.get(j).type == CssToken.Type.BLOCK_START) {
                    j++;
                    while (j < tokens.size() && tokens.get(j).type == CssToken.Type.DECLARATION) {
                        Declaration d = Declaration.parse(tokens.get(j).text);
                        if (d != null) decls.add(d);
                        j++;
                    }
                    if (j < tokens.size() && tokens.get(j).type == CssToken.Type.BLOCK_END) {
                        j++;
                    }
                }
                if (!decls.isEmpty()) {
                    for (String selText : splitSelectorGroup(t.text)) {
                        Selector sel = Selector.parse(selText);
                        if (sel != null) {
                            rules.add(new StyleRule(sel, decls, order));
                        }
                    }
                }
                order++;
                i = j;
            } else {
                // AT_KEYWORD, stray BLOCK_END, EOF, etc. — skip.
                i++;
            }
        }
        return rules;
    }

    /** Splits a selector prelude on top-level commas (respecting {@code [..]}/strings). */
    static List<String> splitSelectorGroup(String prelude) {
        List<String> out = new ArrayList<String>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < prelude.length(); i++) {
            char c = prelude.charAt(i);
            if (c == '[' || c == '(') depth++;
            else if (c == ']' || c == ')') { if (depth > 0) depth--; }
            else if (c == '"' || c == '\'') {
                int j = i + 1;
                while (j < prelude.length() && prelude.charAt(j) != c) j++;
                i = j;
            } else if (c == ',' && depth == 0) {
                String piece = prelude.substring(start, i).trim();
                if (!piece.isEmpty()) out.add(piece);
                start = i + 1;
            }
        }
        String last = prelude.substring(start).trim();
        if (!last.isEmpty()) out.add(last);
        return out;
    }
}
