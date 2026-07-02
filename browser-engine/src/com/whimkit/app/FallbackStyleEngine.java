package com.whimkit.app;

import com.whimkit.css.ComputedStyle;
import com.whimkit.css.StyleEngine;
import com.whimkit.dom.Document;
import com.whimkit.dom.Element;
import com.whimkit.dom.Node;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A User-Agent-defaults-only style engine used <em>only</em> when the full
 * {@code com.whimkit.css.engine.CascadingStyleEngine} subsystem is absent. It
 * applies a built-in default stylesheet (block vs inline display, heading sizes,
 * link/bold/italic, common margins) so pages are legible without author CSS. The
 * real cascade engine (built as a sibling task) supersedes it via
 * {@link Subsystems}. Author CSS passed to {@link #addAuthorCss} is ignored here.
 */
final class FallbackStyleEngine implements StyleEngine {

    private static final Set<String> BLOCK = new HashSet<String>(Arrays.asList(
            "html", "body", "div", "p", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li",
            "section", "article", "header", "footer", "nav", "main", "aside", "figure",
            "blockquote", "pre", "table", "tr", "form", "fieldset", "hr", "dl", "dt", "dd"));

    @Override public void addAuthorCss(String css) { /* ignored in fallback */ }
    @Override public void reset() { }

    @Override
    public void styleDocument(Document doc) {
        Element root = doc.getDocumentElement();
        if (root == null) return;
        style(root, ComputedStyle.initial());
    }

    private void style(Element e, ComputedStyle parent) {
        ComputedStyle s = parent.deriveChild();
        String tag = e.getTagName();

        s.display = BLOCK.contains(tag) ? "block" : "inline";
        if ("li".equals(tag)) s.display = "list-item";
        if ("head".equals(tag) || "title".equals(tag) || "meta".equals(tag)
                || "link".equals(tag) || "script".equals(tag) || "style".equals(tag)) {
            s.display = "none";
        }

        if ("body".equals(tag)) { s.marginTop = s.marginRight = s.marginBottom = s.marginLeft = 8; }
        if ("p".equals(tag) || "blockquote".equals(tag)) { s.marginTop = s.marginBottom = 12; }
        if ("ul".equals(tag) || "ol".equals(tag)) { s.marginTop = s.marginBottom = 12; s.paddingLeft = 30; }
        if ("pre".equals(tag)) { s.whiteSpace = "pre"; s.fontFamily = "Monospaced"; s.marginTop = s.marginBottom = 10; }

        switch (tag) {
            case "h1": s.fontSize = 32; s.fontWeight = 700; s.marginTop = s.marginBottom = 16; break;
            case "h2": s.fontSize = 26; s.fontWeight = 700; s.marginTop = s.marginBottom = 14; break;
            case "h3": s.fontSize = 22; s.fontWeight = 700; s.marginTop = s.marginBottom = 12; break;
            case "h4": s.fontSize = 18; s.fontWeight = 700; s.marginTop = s.marginBottom = 12; break;
            case "h5": s.fontSize = 15; s.fontWeight = 700; break;
            case "h6": s.fontSize = 13; s.fontWeight = 700; break;
            case "b": case "strong": s.fontWeight = 700; break;
            case "i": case "em": s.italic = true; break;
            case "small": s.fontSize = Math.max(9, parent.fontSize - 3); break;
            case "code": case "kbd": case "samp": s.fontFamily = "Monospaced"; break;
            case "a":
                if (e.getAttribute("href") != null) { s.color = new Color(0x1A0DAB); s.underline = true; }
                break;
            default: break;
        }
        // Inline style="color:...;font-weight:bold" minimal handling.
        applyInline(e.getAttribute("style"), s);
        e.setComputedStyle(s);
        for (Node c : e.getChildNodes()) {
            if (c instanceof Element) style((Element) c, s);
        }
    }

    private void applyInline(String style, ComputedStyle s) {
        if (style == null) return;
        for (String decl : style.split(";")) {
            int c = decl.indexOf(':');
            if (c < 0) continue;
            String p = decl.substring(0, c).trim().toLowerCase();
            String v = decl.substring(c + 1).trim();
            try {
                if (p.equals("color")) s.color = Colors.parse(v, s.color);
                else if (p.equals("background-color") || p.equals("background")) s.backgroundColor = Colors.parse(v, s.backgroundColor);
                else if (p.equals("font-weight")) s.fontWeight = v.equals("bold") ? 700 : safeInt(v, s.fontWeight);
                else if (p.equals("font-style")) s.italic = v.contains("italic");
                else if (p.equals("text-align")) s.textAlign = v;
                else if (p.equals("font-size")) s.fontSize = Colors.px(v, s.fontSize);
                else s.raw.put(p, v);
            } catch (RuntimeException ignore) { }
        }
    }

    private static int safeInt(String v, int def) {
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return def; }
    }

    @Override
    public String getPropertyValue(Element element, String property) {
        ComputedStyle s = element == null ? null : element.getComputedStyle();
        if (s == null || property == null) return "";
        String v = s.raw.get(property.toLowerCase());
        return v == null ? "" : v;
    }
}
