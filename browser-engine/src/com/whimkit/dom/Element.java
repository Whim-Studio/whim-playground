package com.whimkit.dom;

import com.whimkit.css.ComputedStyle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An element node ({@code <div>}, {@code <p>}, ...).
 *
 * <p>Tag names are stored lower-cased so matching is case-insensitive throughout
 * the engine. Attributes preserve insertion order via {@link LinkedHashMap}.</p>
 *
 * <p>Two engine-facing hooks live here as fields the pipeline populates:</p>
 * <ul>
 *   <li>{@link #getComputedStyle()} / {@link #setComputedStyle} — set by the CSS
 *       engine after the cascade so layout and rendering can read resolved
 *       values without re-running selector matching.</li>
 * </ul>
 */
public class Element extends Node {

    private final String tagName;
    private final Map<String, String> attributes = new LinkedHashMap<String, String>();
    private ComputedStyle computedStyle;

    public Element(String tagName) {
        this.tagName = tagName == null ? "" : tagName.toLowerCase(Locale.ROOT);
    }

    public String getTagName() {
        return tagName;
    }

    // --- attributes -------------------------------------------------------

    public String getAttribute(String name) {
        if (name == null) return null;
        return attributes.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean hasAttribute(String name) {
        return name != null && attributes.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public void setAttribute(String name, String value) {
        if (name == null) return;
        attributes.put(name.toLowerCase(Locale.ROOT), value == null ? "" : value);
    }

    public void removeAttribute(String name) {
        if (name != null) attributes.remove(name.toLowerCase(Locale.ROOT));
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String getId() {
        return getAttribute("id");
    }

    /** @return the raw {@code class} attribute, or empty string. */
    public String getClassName() {
        String c = getAttribute("class");
        return c == null ? "" : c;
    }

    /** @return whitespace-split class tokens. */
    public List<String> getClassList() {
        List<String> out = new ArrayList<String>();
        for (String tok : getClassName().trim().split("\\s+")) {
            if (!tok.isEmpty()) out.add(tok);
        }
        return out;
    }

    // --- style hook -------------------------------------------------------

    public ComputedStyle getComputedStyle() {
        return computedStyle;
    }

    public void setComputedStyle(ComputedStyle style) {
        this.computedStyle = style;
    }

    // --- queries ----------------------------------------------------------

    /** Recursive descendant search by tag name; {@code "*"} matches all elements. */
    public List<Element> getElementsByTagName(String name) {
        List<Element> out = new ArrayList<Element>();
        String target = name == null ? "*" : name.toLowerCase(Locale.ROOT);
        collectByTag(target, out);
        return out;
    }

    private void collectByTag(String target, List<Element> out) {
        for (Node c : getChildNodes()) {
            if (c instanceof Element) {
                Element e = (Element) c;
                if (target.equals("*") || target.equals(e.tagName)) {
                    out.add(e);
                }
                e.collectByTag(target, out);
            }
        }
    }

    /** @return direct child elements (skipping text/comment nodes). */
    public List<Element> getChildElements() {
        List<Element> out = new ArrayList<Element>();
        for (Node c : getChildNodes()) {
            if (c instanceof Element) out.add((Element) c);
        }
        return out;
    }

    @Override
    public short getNodeType() {
        return ELEMENT_NODE;
    }

    @Override
    public String getNodeName() {
        return tagName;
    }

    @Override
    public String toString() {
        return "<" + tagName + (getId() != null ? " id=\"" + getId() + "\"" : "") + ">";
    }
}
