package com.whimkit.app;

import com.whimkit.dom.Document;
import com.whimkit.dom.Element;
import com.whimkit.dom.Node;
import com.whimkit.html.HtmlParser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A minimal, dependency-free HTML parser used <em>only</em> as a safety net when
 * the full {@code com.whimkit.html.Html5Parser} subsystem is not present on the
 * classpath. It is a small tag-soup scanner: enough to show text, links, and
 * basic structure so the browser always renders something. The real parser
 * (built as a sibling task) supersedes it automatically via {@link Subsystems}.
 */
final class FallbackHtmlParser implements HtmlParser {

    private static final Set<String> VOID = new HashSet<String>(Arrays.asList(
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "link", "meta", "param", "source", "track", "wbr"));
    private static final Set<String> HEAD_TAGS = new HashSet<String>(Arrays.asList(
            "title", "meta", "link", "base", "style"));
    private static final Set<String> RAW = new HashSet<String>(Arrays.asList("script", "style", "title"));

    @Override
    public Document parse(String html, String baseUri) {
        Document doc = new Document();
        doc.setBaseUri(baseUri);
        Element htmlEl = doc.createElement("html");
        Element head = doc.createElement("head");
        Element body = doc.createElement("body");
        htmlEl.appendChild(head);
        htmlEl.appendChild(body);
        doc.appendChild(htmlEl);
        if (html == null) return doc;

        Element cur = body;
        int i = 0, n = html.length();
        try {
            while (i < n) {
                char ch = html.charAt(i);
                if (ch == '<') {
                    if (html.startsWith("<!--", i)) {
                        int end = html.indexOf("-->", i + 4);
                        i = (end < 0) ? n : end + 3;
                        continue;
                    }
                    if (i + 1 < n && html.charAt(i + 1) == '!') {
                        int end = html.indexOf('>', i);
                        i = (end < 0) ? n : end + 1;
                        continue;
                    }
                    int gt = html.indexOf('>', i);
                    if (gt < 0) break;
                    String tag = html.substring(i + 1, gt).trim();
                    i = gt + 1;
                    if (tag.startsWith("/")) {
                        String name = tag.substring(1).trim().toLowerCase();
                        cur = closeTo(body, cur, name);
                    } else {
                        boolean selfClose = tag.endsWith("/");
                        if (selfClose) tag = tag.substring(0, tag.length() - 1).trim();
                        String name = tagName(tag).toLowerCase();
                        if (name.isEmpty()) continue;
                        Element el = doc.createElement(name);
                        applyAttrs(el, tag);
                        Element parent = HEAD_TAGS.contains(name) ? head : cur;
                        parent.appendChild(el);
                        if (RAW.contains(name)) {
                            int end = indexOfIgnoreCase(html, "</" + name, i);
                            String raw = html.substring(i, end < 0 ? n : end);
                            if ("title".equals(name)) doc.setTitle(raw.trim());
                            else el.appendChild(doc.createTextNode(raw));
                            int close = html.indexOf('>', end < 0 ? n : end);
                            i = (close < 0) ? n : close + 1;
                        } else if (!selfClose && !VOID.contains(name) && !HEAD_TAGS.contains(name)) {
                            cur = el;
                        }
                    }
                } else {
                    int lt = html.indexOf('<', i);
                    String text = html.substring(i, lt < 0 ? n : lt);
                    if (!text.trim().isEmpty()) {
                        cur.appendChild(doc.createTextNode(unescape(text)));
                    }
                    i = (lt < 0) ? n : lt;
                }
            }
        } catch (RuntimeException ex) {
            System.err.println("[fallback-html] recovered: " + ex);
        }
        return doc;
    }

    private Element closeTo(Element body, Element cur, String name) {
        Element e = cur;
        while (e != null && e != body) {
            if (e.getTagName().equals(name)) {
                Node p = e.getParentNode();
                return (p instanceof Element) ? (Element) p : body;
            }
            Node p = e.getParentNode();
            e = (p instanceof Element) ? (Element) p : null;
        }
        return cur;
    }

    private static String tagName(String tag) {
        int sp = 0;
        while (sp < tag.length() && !Character.isWhitespace(tag.charAt(sp))) sp++;
        return tag.substring(0, sp);
    }

    private static void applyAttrs(Element el, String tag) {
        String rest = tag.substring(tagName(tag).length());
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*(=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+))?")
                .matcher(rest);
        while (m.find()) {
            String k = m.group(1);
            String v = m.group(3);
            if (v != null) {
                if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                    v = v.substring(1, v.length() - 1);
                }
            } else {
                v = "";
            }
            el.setAttribute(k, unescape(v));
        }
    }

    private static int indexOfIgnoreCase(String s, String needle, int from) {
        return s.toLowerCase().indexOf(needle.toLowerCase(), from);
    }

    private static String unescape(String s) {
        if (s.indexOf('&') < 0) return s;
        return s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&nbsp;", " ");
    }
}
