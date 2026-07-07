package com.whim.albion.data;

import com.whim.albion.api.Content;
import com.whim.albion.api.GameContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A concrete, navigable conversation tree. Nodes are keyed by id; each option
 * carries an optional {@link GameContext} side effect and an availability
 * predicate so replies can be gated on flags / carried items / quest state.
 */
public final class DialogueTreeImpl implements Content.DialogueTree {

    private final String rootId;
    private final Map<String, NodeImpl> nodes = new HashMap<String, NodeImpl>();

    public DialogueTreeImpl(String rootId) { this.rootId = rootId; }

    public NodeImpl addNode(String id, String speaker, String portraitKey, String text) {
        NodeImpl n = new NodeImpl(id, speaker, portraitKey, text);
        nodes.put(id, n);
        return n;
    }

    @Override public String rootId() { return rootId; }
    @Override public Node node(String nodeId) { return nodes.get(nodeId); }

    // -------------------------------------------------------------- node impl

    public static final class NodeImpl implements Node {
        private final String id;
        private final String speaker;
        private final String portraitKey;
        private final String text;
        private final List<Option> options = new ArrayList<Option>();

        NodeImpl(String id, String speaker, String portraitKey, String text) {
            this.id = id;
            this.speaker = speaker;
            this.portraitKey = portraitKey;
            this.text = text;
        }

        /** Add an option with a side effect and an availability predicate. */
        public NodeImpl option(String label, String next, Consumer<GameContext> apply, Predicate<GameContext> available) {
            options.add(new OptionImpl(label, next, apply, available));
            return this;
        }

        /** Always-available option with a side effect. */
        public NodeImpl option(String label, String next, Consumer<GameContext> apply) {
            return option(label, next, apply, null);
        }

        /** Always-available option with no side effect (pure navigation). */
        public NodeImpl option(String label, String next) {
            return option(label, next, null, null);
        }

        @Override public String id() { return id; }
        @Override public String speaker() { return speaker; }
        @Override public String portraitKey() { return portraitKey; }
        @Override public String text() { return text; }
        @Override public List<Option> options() { return Collections.unmodifiableList(options); }
    }

    // ------------------------------------------------------------ option impl

    private static final class OptionImpl implements Option {
        private final String label;
        private final String next;
        private final Consumer<GameContext> apply;
        private final Predicate<GameContext> available;

        OptionImpl(String label, String next, Consumer<GameContext> apply, Predicate<GameContext> available) {
            this.label = label;
            this.next = next;
            this.apply = apply;
            this.available = available;
        }

        @Override public String label() { return label; }
        @Override public String next() { return next; }

        @Override public void apply(GameContext ctx) {
            if (apply != null) apply.accept(ctx);
        }

        @Override public boolean available(GameContext ctx) {
            return available == null || available.test(ctx);
        }
    }
}
