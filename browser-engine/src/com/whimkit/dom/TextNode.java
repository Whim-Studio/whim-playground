package com.whimkit.dom;

/** A character-data node. Holds the raw text; whitespace collapsing is the layout engine's job. */
public class TextNode extends Node {

    private String data;

    public TextNode(String data) {
        this.data = data == null ? "" : data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data == null ? "" : data;
    }

    @Override
    public short getNodeType() {
        return TEXT_NODE;
    }

    @Override
    public String getNodeName() {
        return "#text";
    }

    @Override
    void collectText(StringBuilder sb) {
        sb.append(data);
    }
}
