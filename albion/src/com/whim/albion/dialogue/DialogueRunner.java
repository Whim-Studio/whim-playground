package com.whim.albion.dialogue;

import com.whim.albion.api.Content;
import com.whim.albion.api.GameContext;
import com.whim.albion.api.Views.DialogueView;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs a {@link Content.DialogueTree}: tracks the current node, projects a
 * {@link DialogueView} for the UI, and applies an option's {@link GameContext}
 * side effects when selected.
 *
 * <p>Only options whose {@code available(ctx)} is true are shown; the selection
 * index the UI passes therefore refers to the <em>filtered</em> options list, so
 * we re-filter on every read and selection to stay consistent.</p>
 */
public final class DialogueRunner {

    private final Content.DialogueTree tree;
    private final GameContext ctx;
    private Content.DialogueTree.Node node;

    public DialogueRunner(Content.DialogueTree tree, GameContext ctx) {
        this.tree = tree;
        this.ctx = ctx;
        this.node = tree.node(tree.rootId());
    }

    /** Currently available options at the current node. */
    private List<Content.DialogueTree.Option> availableOptions() {
        List<Content.DialogueTree.Option> out = new ArrayList<Content.DialogueTree.Option>();
        if (node == null) return out;
        List<Content.DialogueTree.Option> all = node.options();
        if (all == null) return out;
        for (int i = 0; i < all.size(); i++) {
            Content.DialogueTree.Option o = all.get(i);
            if (o != null && o.available(ctx)) out.add(o);
        }
        return out;
    }

    /**
     * Select the {@code index}-th available option: apply its effects and advance
     * to its next node (or end).
     *
     * @return true if the conversation has ended (no node / null next).
     */
    public boolean select(int index) {
        List<Content.DialogueTree.Option> opts = availableOptions();
        // A node with no available options is terminal: any acknowledgement ends it.
        if (opts.isEmpty()) { node = null; return true; }
        if (index < 0 || index >= opts.size()) return false;   // invalid pick, stay put
        Content.DialogueTree.Option chosen = opts.get(index);
        chosen.apply(ctx);
        String next = chosen.next();
        if (next == null) { node = null; return true; }
        node = tree.node(next);
        if (node == null) return true;
        return false;
    }

    /** Whether the conversation has ended. */
    public boolean ended() { return node == null; }

    /** Read-only projection for the UI. */
    public DialogueView view() { return new ViewImpl(); }

    private final class ViewImpl implements DialogueView {
        @Override public String speaker() { return node == null ? "" : safe(node.speaker()); }
        @Override public String portraitKey() { return node == null ? "" : safe(node.portraitKey()); }
        @Override public String text() { return node == null ? "" : safe(node.text()); }
        @Override public List<String> options() {
            List<String> labels = new ArrayList<String>();
            List<Content.DialogueTree.Option> opts = availableOptions();
            for (int i = 0; i < opts.size(); i++) labels.add(safe(opts.get(i).label()));
            return labels;
        }
        private String safe(String s) { return s == null ? "" : s; }
    }
}
