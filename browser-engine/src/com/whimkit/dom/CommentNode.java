package com.whimkit.dom;

/** An HTML comment. Preserved in the tree but never rendered. */
public class CommentNode extends Node {

    private final String data;

    public CommentNode(String data) {
        this.data = data == null ? "" : data;
    }

    public String getData() {
        return data;
    }

    @Override
    public short getNodeType() {
        return COMMENT_NODE;
    }

    @Override
    public String getNodeName() {
        return "#comment";
    }

    @Override
    void collectText(StringBuilder sb) {
        // Comments contribute no rendered text.
    }
}
