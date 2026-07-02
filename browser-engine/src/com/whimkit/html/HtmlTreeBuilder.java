package com.whimkit.html;

import com.whimkit.dom.Document;
import com.whimkit.dom.Element;
import com.whimkit.dom.Node;
import com.whimkit.dom.TextNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Consumes the {@link HtmlToken} stream and constructs a foundation
 * {@link Document}.
 *
 * <p>This is a simplified insertion-mode machine built around an <em>open-element
 * stack</em>. It implements the parts of the WHATWG tree-construction algorithm
 * that matter for turning real-world (and frequently malformed) markup into a
 * sane tree:</p>
 *
 * <ul>
 *   <li>implied {@code <html>}, {@code <head>}, and {@code <body>} creation;</li>
 *   <li>the void-element set (never given children);</li>
 *   <li>optional-end-tag auto-closing for {@code <p>}, {@code <li>}, {@code <dd>},
 *       {@code <dt>}, {@code <option>}, and the table family
 *       ({@code <tr>}/{@code <td>}/{@code <th>}/{@code <thead>}/{@code <tbody>}/
 *       {@code <tfoot>});</li>
 *   <li>misnest recovery: an end tag with no matching open element is ignored,
 *       and a matching one closes every intervening element, so bad nesting
 *       still yields a well-formed tree;</li>
 *   <li>comment attachment, {@code <title>} capture into {@link Document#setTitle},
 *       and dropping of pure-whitespace text in table contexts.</li>
 * </ul>
 *
 * <p>The builder never throws on bad input. It is single-use and single-threaded;
 * the {@link Document} it produces is likewise not thread-safe.</p>
 */
final class HtmlTreeBuilder {

    /** Elements that never receive children. */
    private static final Set<String> VOID = setOf(
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "link", "meta", "param", "source", "track", "wbr");

    /** Elements that live in {@code <head>} when they appear before {@code <body>}. */
    private static final Set<String> HEAD_CONTENT = setOf(
            "base", "basefont", "bgsound", "link", "meta", "title",
            "style", "script", "noscript", "template");

    /** Start tags that imply closing an open {@code <p>}. */
    private static final Set<String> CLOSES_P = setOf(
            "address", "article", "aside", "blockquote", "details", "div", "dl",
            "dd", "dt", "fieldset", "figcaption", "figure", "footer", "form",
            "h1", "h2", "h3", "h4", "h5", "h6", "header", "hgroup", "hr", "main",
            "menu", "nav", "ol", "p", "pre", "section", "table", "ul", "li");

    /** Containers whose direct pure-whitespace text is dropped. */
    private static final Set<String> WS_DROP_CONTEXT = setOf(
            "table", "thead", "tbody", "tfoot", "tr", "colgroup", "select");

    /** Elements whose text content is opaque (must not trigger body insertion). */
    private static final Set<String> TEXT_CONTAINER = setOf(
            "script", "style", "title", "textarea");

    private enum Mode { INITIAL, IN_HEAD, IN_BODY, AFTER_BODY }

    private final Document doc = new Document();
    private final List<Element> openStack = new ArrayList<Element>();

    private Element htmlEl;
    private Element headEl;
    private Element bodyEl;
    private Mode mode = Mode.INITIAL;

    /**
     * Builds the document from a token stream.
     *
     * @param tokens  the tokenizer output (must end with an EOF token).
     * @param baseUri the document base URI to record.
     * @return the constructed document (never {@code null}).
     */
    Document build(List<HtmlToken> tokens, String baseUri) {
        doc.setBaseUri(baseUri == null ? "" : baseUri);
        for (HtmlToken t : tokens) {
            try {
                dispatch(t);
            } catch (RuntimeException ex) {
                // Tree construction is best-effort: never let one malformed
                // token abort the whole parse.
            }
        }
        finish();
        return doc;
    }

    private void dispatch(HtmlToken t) {
        switch (t.type) {
            case START_TAG:  handleStartTag(t); break;
            case END_TAG:    handleEndTag(t.name); break;
            case CHARACTER:  handleCharacters(t.data); break;
            case COMMENT:    handleComment(t.data); break;
            case DOCTYPE:    /* recorded implicitly; no doctype node type */ break;
            case EOF:        break;
            default:         break;
        }
    }

    // --- start tags -------------------------------------------------------

    private void handleStartTag(HtmlToken t) {
        String name = t.name;
        if (name.isEmpty()) return;

        if (name.equals("html")) {
            ensureHtml();
            mergeAttributes(htmlEl, t);
            return;
        }
        if (name.equals("head")) {
            ensureHtml();
            if (headEl == null) {
                headEl = doc.createElement("head");
                htmlEl.appendChild(headEl);
                push(headEl);
                mode = Mode.IN_HEAD;
            }
            mergeAttributes(headEl, t);
            return;
        }
        if (name.equals("body")) {
            ensureBody();
            mergeAttributes(bodyEl, t);
            return;
        }

        // Head-only content encountered before the body opens stays in <head>.
        if (mode != Mode.IN_BODY && HEAD_CONTENT.contains(name)) {
            ensureHeadOpen();
            insertElement(t, VOID.contains(name));
            return;
        }

        // Anything else is body content: transition into the body first.
        ensureBody();
        applyAutoClose(name);
        insertElement(t, VOID.contains(name) || t.selfClosing);
    }

    /** Runs the optional-end-tag / misnest auto-close rules for a new start tag. */
    private void applyAutoClose(String name) {
        if (name.equals("li")) {
            autoClose(setOf("li"), setOf("ul", "ol", "html", "body"));
        } else if (name.equals("dd") || name.equals("dt")) {
            autoClose(setOf("dd", "dt"), setOf("dl", "html", "body"));
        } else if (name.equals("option")) {
            autoClose(setOf("option"), setOf("select", "optgroup", "html", "body"));
        } else if (name.equals("optgroup")) {
            autoClose(setOf("option", "optgroup"), setOf("select", "html", "body"));
        } else if (name.equals("tr")) {
            popUntilCurrentIn(setOf("table", "thead", "tbody", "tfoot", "template", "html"));
        } else if (name.equals("td") || name.equals("th")) {
            popUntilCurrentIn(setOf("tr", "template", "html"));
        } else if (name.equals("thead") || name.equals("tbody") || name.equals("tfoot")) {
            popUntilCurrentIn(setOf("table", "template", "html"));
        }

        if (CLOSES_P.contains(name)) {
            closePInScope();
        }
    }

    /** Creates the element for {@code t}, appends it, and pushes it unless it is void. */
    private void insertElement(HtmlToken t, boolean voidLike) {
        Element e = doc.createElement(t.name);
        if (t.attributes != null) {
            for (HtmlToken.Attr a : t.attributes) {
                e.setAttribute(a.name, a.value);
            }
        }
        current().appendChild(e);
        if (!voidLike) push(e);
    }

    // --- end tags ---------------------------------------------------------

    private void handleEndTag(String name) {
        if (name.isEmpty() || VOID.contains(name)) return;

        if (name.equals("head")) {
            if (mode == Mode.IN_HEAD) {
                popElement(headEl);
                mode = Mode.INITIAL; // between head and body
            }
            return;
        }
        if (name.equals("body") || name.equals("html")) {
            // Close everything back down to the body; further content is tolerated.
            if (bodyEl != null) {
                popTo(bodyEl);
            }
            mode = Mode.AFTER_BODY;
            return;
        }

        // Find the nearest matching open element and pop through it.
        int idx = indexOfOpen(name);
        if (idx < 0) return; // misnest recovery: no match -> ignore
        while (openStack.size() > idx) {
            openStack.remove(openStack.size() - 1);
        }
    }

    // --- character data & comments ---------------------------------------

    private void handleCharacters(String text) {
        if (text == null || text.isEmpty()) return;

        Element cur = current();
        String curTag = (cur == null || cur == doc.getDocumentElement()) ? null : cur.getTagName();

        // Text inside an opaque container (script/style/title/textarea) is kept raw.
        if (cur != null && TEXT_CONTAINER.contains(cur.getTagName())) {
            appendText(cur, text);
            return;
        }

        boolean whitespace = isAllWhitespace(text);

        if (mode != Mode.IN_BODY) {
            if (whitespace) return; // drop inter-element whitespace before the body
            ensureBody();
            cur = current();
        }

        if (whitespace && WS_DROP_CONTEXT.contains(current().getTagName())) {
            return; // drop pure-whitespace directly inside table/select contexts
        }
        appendText(current(), text);
    }

    private void handleComment(String data) {
        Node target = openStack.isEmpty() ? doc : current();
        target.appendChild(doc.createComment(data == null ? "" : data));
    }

    // --- structural helpers ----------------------------------------------

    private void ensureHtml() {
        if (htmlEl == null) {
            htmlEl = doc.createElement("html");
            doc.appendChild(htmlEl);
            push(htmlEl);
        }
    }

    private void ensureHeadOpen() {
        ensureHtml();
        if (headEl == null) {
            headEl = doc.createElement("head");
            htmlEl.appendChild(headEl);
            push(headEl);
            mode = Mode.IN_HEAD;
        }
    }

    private void ensureBody() {
        if (mode == Mode.IN_BODY || mode == Mode.AFTER_BODY) {
            if (bodyEl != null) return;
        }
        ensureHtml();
        // Close the head (and anything left open in it) back down to <html>.
        popTo(htmlEl);
        if (bodyEl == null) {
            bodyEl = doc.createElement("body");
            htmlEl.appendChild(bodyEl);
        }
        if (!openStack.contains(bodyEl)) push(bodyEl);
        mode = Mode.IN_BODY;
    }

    /** Closes an open {@code <p>} (with implied end tags) if one is in scope. */
    private void closePInScope() {
        Set<String> boundary = setOf("td", "th", "caption", "table", "html", "body");
        boolean found = false;
        for (int i = openStack.size() - 1; i >= 0; i--) {
            String tag = openStack.get(i).getTagName();
            if (tag.equals("p")) { found = true; break; }
            if (boundary.contains(tag)) return;
        }
        if (!found) return;
        while (!openStack.isEmpty()) {
            Element e = openStack.remove(openStack.size() - 1);
            if (e.getTagName().equals("p")) break;
        }
    }

    /**
     * Pops elements until one whose tag is in {@code targets} has been popped,
     * but abandons the search (popping nothing) if a {@code boundaries} tag is
     * reached first. Implements list-item / definition-item auto-closing.
     */
    private void autoClose(Set<String> targets, Set<String> boundaries) {
        boolean found = false;
        for (int i = openStack.size() - 1; i >= 0; i--) {
            String tag = openStack.get(i).getTagName();
            if (targets.contains(tag)) { found = true; break; }
            if (boundaries.contains(tag)) return;
        }
        if (!found) return;
        while (!openStack.isEmpty()) {
            Element e = openStack.remove(openStack.size() - 1);
            if (targets.contains(e.getTagName())) break;
        }
    }

    /** Pops elements until the current node's tag is in {@code stop} (table flow). */
    private void popUntilCurrentIn(Set<String> stop) {
        while (!openStack.isEmpty() && !stop.contains(current().getTagName())) {
            openStack.remove(openStack.size() - 1);
        }
    }

    private void finish() {
        // Guarantee a minimally valid tree even for empty/degenerate input.
        ensureHtml();
        if (headEl == null) {
            headEl = doc.createElement("head");
            htmlEl.insertBefore(headEl, htmlEl.getFirstChild());
        }
        if (bodyEl == null) {
            bodyEl = doc.createElement("body");
            htmlEl.appendChild(bodyEl);
        }
        captureTitle();
    }

    /** Sets {@link Document#setTitle} from the first {@code <title>}'s text. */
    private void captureTitle() {
        List<Element> titles = doc.getElementsByTagName("title");
        if (titles.isEmpty()) return;
        String text = titles.get(0).getTextContent();
        // Collapse internal whitespace runs for a clean tab/window title.
        doc.setTitle(text.trim().replaceAll("\\s+", " "));
    }

    // --- open-element stack primitives ------------------------------------

    private Element current() {
        return openStack.isEmpty() ? htmlEl : openStack.get(openStack.size() - 1);
    }

    private void push(Element e) {
        openStack.add(e);
    }

    private int indexOfOpen(String tag) {
        for (int i = openStack.size() - 1; i >= 0; i--) {
            if (openStack.get(i).getTagName().equals(tag)) return i;
        }
        return -1;
    }

    private void popElement(Element e) {
        int i = openStack.lastIndexOf(e);
        if (i >= 0) {
            while (openStack.size() > i) openStack.remove(openStack.size() - 1);
        }
    }

    /** Pops everything above {@code e} (exclusive), leaving {@code e} on top. */
    private void popTo(Element e) {
        int i = openStack.lastIndexOf(e);
        if (i < 0) return;
        while (openStack.size() > i + 1) openStack.remove(openStack.size() - 1);
    }

    // --- small utilities --------------------------------------------------

    private static void appendText(Element parent, String text) {
        Node last = parent.getLastChild();
        if (last instanceof TextNode) {
            TextNode tn = (TextNode) last;
            tn.setData(tn.getData() + text);
        } else {
            parent.appendChild(parent.getOwnerDocument().createTextNode(text));
        }
    }

    private static void mergeAttributes(Element e, HtmlToken t) {
        if (e == null || t.attributes == null) return;
        for (HtmlToken.Attr a : t.attributes) {
            if (!e.hasAttribute(a.name)) e.setAttribute(a.name, a.value);
        }
    }

    private static boolean isAllWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r' && c != '\f') return false;
        }
        return true;
    }

    private static Set<String> setOf(String... items) {
        return new HashSet<String>(Arrays.asList(items));
    }
}
