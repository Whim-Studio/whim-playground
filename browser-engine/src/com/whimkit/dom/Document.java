package com.whimkit.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * The document root. Owns factory methods and whole-tree queries.
 *
 * <p>{@code baseUri} is stored so relative URLs in {@code <a href>},
 * {@code <img src>}, {@code <link href>}, etc. can be resolved by the networking
 * layer during a load.</p>
 */
public class Document extends Node {

    private String baseUri = "";
    private String title = "";

    public Document() {
        this.ownerDocument = this;
    }

    // --- factories --------------------------------------------------------

    public Element createElement(String tagName) {
        Element e = new Element(tagName);
        e.ownerDocument = this;
        return e;
    }

    public TextNode createTextNode(String data) {
        TextNode t = new TextNode(data);
        t.ownerDocument = this;
        return t;
    }

    public CommentNode createComment(String data) {
        CommentNode c = new CommentNode(data);
        c.ownerDocument = this;
        return c;
    }

    // --- tree accessors ---------------------------------------------------

    /** @return the root {@code <html>} element, or the first element child. */
    public Element getDocumentElement() {
        for (Node c : getChildNodes()) {
            if (c instanceof Element) return (Element) c;
        }
        return null;
    }

    public Element getBody() {
        return firstByTag("body");
    }

    public Element getHead() {
        return firstByTag("head");
    }

    private Element firstByTag(String tag) {
        Element root = getDocumentElement();
        if (root == null) return null;
        if (root.getTagName().equals(tag)) return root;
        List<Element> found = root.getElementsByTagName(tag);
        return found.isEmpty() ? null : found.get(0);
    }

    public Element getElementById(String id) {
        if (id == null) return null;
        Element root = getDocumentElement();
        return root == null ? null : findById(root, id);
    }

    private Element findById(Element e, String id) {
        if (id.equals(e.getId())) return e;
        for (Element child : e.getChildElements()) {
            Element r = findById(child, id);
            if (r != null) return r;
        }
        return null;
    }

    public List<Element> getElementsByTagName(String name) {
        Element root = getDocumentElement();
        if (root == null) return new ArrayList<Element>();
        List<Element> out = new ArrayList<Element>();
        if (name == null || name.equals("*") || root.getTagName().equalsIgnoreCase(name)) {
            out.add(root);
        }
        out.addAll(root.getElementsByTagName(name));
        return out;
    }

    public String getBaseUri() {
        return baseUri;
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri == null ? "" : baseUri;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    @Override
    public short getNodeType() {
        return DOCUMENT_NODE;
    }

    @Override
    public String getNodeName() {
        return "#document";
    }
}
