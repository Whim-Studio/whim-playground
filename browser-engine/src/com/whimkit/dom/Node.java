package com.whimkit.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for every node in the document tree.
 *
 * <p>This is a <em>foundation data type</em>: it is authored once and shared by
 * every engine subsystem. The HTML tree builder constructs {@code Node}s, the
 * CSS engine annotates {@link Element}s with a {@link com.whimkit.css.ComputedStyle},
 * the layout engine reads the tree, and the Nashorn bridge mutates it. Keeping
 * the tree structure here (and the algorithms in their own packages) is what
 * lets each subsystem compile independently.</p>
 *
 * <p>The node model is intentionally a pragmatic subset of the W3C DOM: parent
 * pointer, ordered child list, and the mutation primitives the rest of the
 * engine actually needs. It is <strong>not</strong> thread-safe; all DOM
 * mutation is expected to happen on a single thread (the EDT or the JS thread,
 * never both at once).</p>
 */
public abstract class Node {

    /** {@code nodeType} constants mirroring the DOM spec values used by the engine. */
    public static final short ELEMENT_NODE = 1;
    public static final short TEXT_NODE = 3;
    public static final short COMMENT_NODE = 8;
    public static final short DOCUMENT_NODE = 9;
    public static final short DOCTYPE_NODE = 10;

    private Node parent;
    private final List<Node> children = new ArrayList<Node>();
    Document ownerDocument;

    /** @return the DOM node-type constant for this node. */
    public abstract short getNodeType();

    /** @return a diagnostic node name (tag name, {@code #text}, {@code #comment}, ...). */
    public abstract String getNodeName();

    public Node getParentNode() {
        return parent;
    }

    /** @return a defensive copy-free live view of the child list; do not mutate directly. */
    public List<Node> getChildNodes() {
        return children;
    }

    public boolean hasChildNodes() {
        return !children.isEmpty();
    }

    public Node getFirstChild() {
        return children.isEmpty() ? null : children.get(0);
    }

    public Node getLastChild() {
        return children.isEmpty() ? null : children.get(children.size() - 1);
    }

    public Node getNextSibling() {
        if (parent == null) return null;
        int i = parent.children.indexOf(this);
        return (i >= 0 && i + 1 < parent.children.size()) ? parent.children.get(i + 1) : null;
    }

    public Node getPreviousSibling() {
        if (parent == null) return null;
        int i = parent.children.indexOf(this);
        return (i > 0) ? parent.children.get(i - 1) : null;
    }

    public Document getOwnerDocument() {
        return ownerDocument;
    }

    /**
     * Appends {@code child}, detaching it from any previous parent first so the
     * tree never develops two parents for one node.
     *
     * @return the appended child (mirrors {@code Node.appendChild}).
     */
    public Node appendChild(Node child) {
        if (child == null) throw new IllegalArgumentException("child == null");
        if (child == this) throw new IllegalStateException("cannot append node to itself");
        child.detach();
        child.parent = this;
        child.setOwnerDocumentRecursive(this.ownerDocument);
        children.add(child);
        return child;
    }

    /** Inserts {@code child} before {@code ref}; appends if {@code ref} is null or absent. */
    public Node insertBefore(Node child, Node ref) {
        if (child == null) throw new IllegalArgumentException("child == null");
        int idx = (ref == null) ? -1 : children.indexOf(ref);
        if (idx < 0) return appendChild(child);
        child.detach();
        child.parent = this;
        child.setOwnerDocumentRecursive(this.ownerDocument);
        children.add(idx, child);
        return child;
    }

    public Node removeChild(Node child) {
        if (children.remove(child)) {
            child.parent = null;
        }
        return child;
    }

    /** Removes this node from its parent, if any. */
    public void detach() {
        if (parent != null) {
            parent.removeChild(this);
        }
    }

    void setOwnerDocumentRecursive(Document doc) {
        this.ownerDocument = doc;
        for (Node c : children) {
            c.setOwnerDocumentRecursive(doc);
        }
    }

    /**
     * @return the concatenated text of this node and its descendants, matching
     *         the DOM {@code textContent} accessor closely enough for rendering.
     */
    public String getTextContent() {
        StringBuilder sb = new StringBuilder();
        collectText(sb);
        return sb.toString();
    }

    void collectText(StringBuilder sb) {
        for (Node c : children) {
            c.collectText(sb);
        }
    }
}
