package com.whimkit.css.engine;

import java.util.List;

/**
 * A single matchable rule: one {@link Selector} paired with the declaration
 * block it shares with the other selectors in its comma group.
 *
 * <p>A source CSS rule like {@code h1, h2 { ... }} expands into two
 * {@code StyleRule}s (one per selector) that reference the same declaration
 * list, so each can carry its own specificity while the cascade treats them
 * uniformly.</p>
 */
final class StyleRule {

    final Selector selector;
    final List<Declaration> declarations;
    /** Origin/order rank used to break specificity ties (higher wins). */
    final int order;
    private final int specificity;

    StyleRule(Selector selector, List<Declaration> declarations, int order) {
        this.selector = selector;
        this.declarations = declarations;
        this.order = order;
        this.specificity = selector.specificity();
    }

    int specificity() {
        return specificity;
    }
}
