package com.whimkit.css.engine;

/**
 * A single lexical token produced by {@link CssTokenizer}.
 *
 * <p>The tokenizer emits a deliberately small token vocabulary — just enough to
 * drive the hand-written recursive-descent {@link CssParser}. Selectors and
 * declaration values are re-scanned from the raw text the tokenizer preserves,
 * which keeps the grammar tolerant of the messy real-world CSS this engine has
 * to survive.</p>
 *
 * <p>Instances are immutable. The token model is single-threaded like the rest
 * of the engine.</p>
 */
final class CssToken {

    /** The kinds of token the CSS tokenizer distinguishes. */
    enum Type {
        /** A selector prelude preceding a {@code '{'} (raw, unparsed text). */
        SELECTOR,
        /** The {@code '{'} that opens a declaration or at-rule block. */
        BLOCK_START,
        /** The {@code '}'} that closes a block. */
        BLOCK_END,
        /** A single {@code property:value} declaration (raw text, no trailing {@code ';'}). */
        DECLARATION,
        /** An at-rule prelude, e.g. {@code @media screen} or {@code @import "x"}. */
        AT_KEYWORD,
        /** End of input. */
        EOF
    }

    final Type type;
    final String text;

    CssToken(Type type, String text) {
        this.type = type;
        this.text = text == null ? "" : text;
    }

    @Override
    public String toString() {
        return type + "(" + text + ")";
    }
}
