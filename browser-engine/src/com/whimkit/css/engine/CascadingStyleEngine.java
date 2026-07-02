package com.whimkit.css.engine;

import com.whimkit.css.ComputedStyle;
import com.whimkit.css.StyleEngine;
import com.whimkit.dom.Document;
import com.whimkit.dom.Element;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The WhimKit CSS engine: parses stylesheets and runs selector matching,
 * specificity, the cascade and inheritance to attach a {@link ComputedStyle} to
 * every {@link Element} in a document.
 *
 * <h3>Cascade model</h3>
 * <p>Three origins participate, applied lowest-to-highest so later wins:</p>
 * <ol>
 *   <li>the built-in {@link UserAgentStylesheet};</li>
 *   <li>author sheets added via {@link #addAuthorCss} (source order preserved);</li>
 *   <li>the element's inline {@code style="..."} attribute.</li>
 * </ol>
 * <p>Within a single element the matched declarations are ordered by
 * {@code (!important, origin, specificity, source-order)} and applied in that
 * order. Inherited properties are seeded from the parent via
 * {@link ComputedStyle#deriveChild()} before declarations overlay on top.</p>
 *
 * <h3>Threading</h3>
 * <p>Like the rest of the engine this class assumes a single-threaded DOM: build
 * or restyle a document from one thread at a time. {@link #addAuthorCss} and
 * {@link #styleDocument} are not synchronised.</p>
 *
 * <h3>Known limitations (documented, not bugs)</h3>
 * <ul>
 *   <li>No pseudo-classes/elements, sibling ({@code +}/{@code ~}) combinators,
 *       or {@code ~=}/{@code ^=}/{@code $=}/{@code *=} attribute matches.</li>
 *   <li>{@code @media}/{@code @supports}/{@code @font-face} blocks are skipped
 *       (not evaluated).</li>
 *   <li>Percentage {@code width}/{@code height}/{@code margin}/{@code padding}
 *       cannot resolve to px without layout, so the typed field is left
 *       {@code auto}/{@code 0} and the raw value is preserved for the layout
 *       engine via {@link #getPropertyValue}.</li>
 *   <li>No flex/grid/animation/transform semantics.</li>
 * </ul>
 */
public final class CascadingStyleEngine implements StyleEngine {

    /** Cascade origins, ordered so a higher ordinal wins ties. */
    private enum Origin { UA, AUTHOR, INLINE }

    private final List<StyleRule> uaRules;
    private final List<StyleRule> authorRules = new ArrayList<StyleRule>();
    private int authorOrderCounter = 0;

    /** Fallback root font size (px) used to resolve {@code rem} before the root is styled. */
    private float rootFontSize = 16f;

    public CascadingStyleEngine() {
        this.uaRules = CssParser.parse(UserAgentStylesheet.css(), 0);
    }

    // --- StyleEngine API --------------------------------------------------

    @Override
    public void addAuthorCss(String css) {
        if (css == null || css.trim().isEmpty()) return;
        List<StyleRule> parsed = CssParser.parse(css, authorOrderCounter);
        authorRules.addAll(parsed);
        // Advance the order counter past this sheet so later sheets sort after it.
        for (StyleRule r : parsed) {
            if (r.order >= authorOrderCounter) authorOrderCounter = r.order + 1;
        }
        authorOrderCounter++;
    }

    @Override
    public void reset() {
        authorRules.clear();
        authorOrderCounter = 0;
    }

    @Override
    public void styleDocument(Document doc) {
        if (doc == null) return;
        Element root = doc.getDocumentElement();
        if (root == null) return;

        // Resolve the root font-size first so rem units are stable tree-wide.
        rootFontSize = 16f;
        ComputedStyle rootStyle = computeStyle(root, null, rootFontSize);
        rootFontSize = rootStyle.fontSize;
        root.setComputedStyle(rootStyle);

        for (Element child : root.getChildElements()) {
            styleSubtree(child, rootStyle);
        }
    }

    @Override
    public String getPropertyValue(Element element, String property) {
        if (element == null || property == null) return "";
        ComputedStyle cs = element.getComputedStyle();
        if (cs == null) return "";
        String prop = property.trim().toLowerCase(Locale.ROOT);
        String reconstructed = reconstruct(cs, prop);
        if (reconstructed != null) return reconstructed;
        String raw = cs.raw.get(prop);
        return raw == null ? "" : raw;
    }

    // --- tree walk --------------------------------------------------------

    private void styleSubtree(Element element, ComputedStyle parentStyle) {
        ComputedStyle cs = computeStyle(element, parentStyle, rootFontSize);
        element.setComputedStyle(cs);
        for (Element child : element.getChildElements()) {
            styleSubtree(child, cs);
        }
    }

    /**
     * Builds the computed style for one element: seed inherited values, collect
     * matching declarations across origins, order them by the cascade, and apply.
     */
    private ComputedStyle computeStyle(Element element, ComputedStyle parentStyle, float rootFont) {
        ComputedStyle cs = (parentStyle == null)
                ? ComputedStyle.initial()
                : parentStyle.deriveChild();

        float parentFontSize = (parentStyle == null) ? 16f : parentStyle.fontSize;

        List<Matched> matched = collectMatched(element);
        Collections.sort(matched, CASCADE_ORDER);

        StyleApplier.Context ctx =
                new StyleApplier.Context(cs.fontSize, parentFontSize, rootFont);

        // Pass 1: settle font-size first so em/ex lengths resolve correctly.
        for (Matched m : matched) {
            if (m.decl.property.equals("font-size") || m.decl.property.equals("font")) {
                StyleApplier.apply(cs, m.decl.property, m.decl.value, ctx);
            }
        }
        ctx.ownFontSize = cs.fontSize;

        // Pass 2: apply everything in cascade order (font-size re-applies identically).
        for (Matched m : matched) {
            StyleApplier.apply(cs, m.decl.property, m.decl.value, ctx);
        }
        return cs;
    }

    /** A declaration that matched an element, tagged with its cascade sort keys. */
    private static final class Matched {
        final Declaration decl;
        final Origin origin;
        final int specificity;
        final int order;
        Matched(Declaration decl, Origin origin, int specificity, int order) {
            this.decl = decl;
            this.origin = origin;
            this.specificity = specificity;
            this.order = order;
        }
    }

    /** Ascending cascade order: earlier entries are applied first (lose to later ones). */
    private static final Comparator<Matched> CASCADE_ORDER = new Comparator<Matched>() {
        @Override
        public int compare(Matched a, Matched b) {
            boolean ai = a.decl.important, bi = b.decl.important;
            if (ai != bi) return ai ? 1 : -1;                 // !important applied last
            if (a.origin != b.origin) return a.origin.ordinal() - b.origin.ordinal();
            if (a.specificity != b.specificity) return a.specificity - b.specificity;
            return a.order - b.order;                          // source order
        }
    };

    private List<Matched> collectMatched(Element element) {
        List<Matched> out = new ArrayList<Matched>();
        for (StyleRule r : uaRules) {
            if (r.selector.matches(element)) {
                addAll(out, r.declarations, Origin.UA, r.specificity(), r.order);
            }
        }
        for (StyleRule r : authorRules) {
            if (r.selector.matches(element)) {
                addAll(out, r.declarations, Origin.AUTHOR, r.specificity(), r.order);
            }
        }
        String inline = element.getAttribute("style");
        if (inline != null && !inline.trim().isEmpty()) {
            int idx = 0;
            for (String raw : splitInline(inline)) {
                Declaration d = Declaration.parse(raw);
                if (d != null) {
                    // Inline wins over selectors: max specificity, highest order.
                    out.add(new Matched(d, Origin.INLINE, Integer.MAX_VALUE, idx++));
                }
            }
        }
        return out;
    }

    private static void addAll(List<Matched> out, List<Declaration> decls,
                               Origin origin, int spec, int order) {
        for (Declaration d : decls) {
            out.add(new Matched(d, origin, spec, order));
        }
    }

    /** Splits an inline {@code style} attribute on {@code ';'} (honouring strings/parens). */
    private static List<String> splitInline(String s) {
        List<String> out = new ArrayList<String>();
        int depth = 0, start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') { if (depth > 0) depth--; }
            else if (c == '"' || c == '\'') {
                int j = i + 1;
                while (j < s.length() && s.charAt(j) != c) j++;
                i = j;
            } else if (c == ';' && depth == 0) {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        out.add(s.substring(start));
        return out;
    }

    // --- getPropertyValue reconstruction ----------------------------------

    /** @return a best-effort computed-value string, or {@code null} to fall back to the raw map. */
    private static String reconstruct(ComputedStyle cs, String prop) {
        switch (prop) {
            case "display": return cs.display;
            case "position": return cs.position;
            case "float": return cs.floatValue;
            case "clear": return cs.clear;
            case "visibility": return cs.visibility;
            case "white-space": return cs.whiteSpace;
            case "text-align": return cs.textAlign;
            case "list-style-type": return cs.listStyleType;
            case "font-family": return cs.fontFamily;
            case "font-size": return px(cs.fontSize);
            case "line-height": return px(cs.lineHeight);
            case "font-weight": return String.valueOf(cs.fontWeight);
            case "font-style": return cs.italic ? "italic" : "normal";
            case "color": return rgb(cs.color);
            case "background-color": return cs.backgroundColor == null ? "transparent" : rgb(cs.backgroundColor);
            case "border-color": return cs.borderColor == null ? "" : rgb(cs.borderColor);
            case "text-decoration":
                if (cs.underline && cs.lineThrough) return "underline line-through";
                if (cs.underline) return "underline";
                if (cs.lineThrough) return "line-through";
                return "none";
            case "width": return cs.width == null ? "auto" : px(cs.width);
            case "height": return cs.height == null ? "auto" : px(cs.height);
            case "margin-top": return px(cs.marginTop);
            case "margin-right": return px(cs.marginRight);
            case "margin-bottom": return px(cs.marginBottom);
            case "margin-left": return px(cs.marginLeft);
            case "padding-top": return px(cs.paddingTop);
            case "padding-right": return px(cs.paddingRight);
            case "padding-bottom": return px(cs.paddingBottom);
            case "padding-left": return px(cs.paddingLeft);
            case "border-top-width": return px(cs.borderTopWidth);
            case "border-right-width": return px(cs.borderRightWidth);
            case "border-bottom-width": return px(cs.borderBottomWidth);
            case "border-left-width": return px(cs.borderLeftWidth);
            default: return null;
        }
    }

    private static String px(float v) {
        if (v == Math.rint(v)) return String.valueOf((int) v) + "px";
        return String.valueOf(v) + "px";
    }

    private static String rgb(Color c) {
        if (c == null) return "";
        if (c.getAlpha() == 255) {
            return "rgb(" + c.getRed() + ", " + c.getGreen() + ", " + c.getBlue() + ")";
        }
        String a = trimFloat(c.getAlpha() / 255f);
        return "rgba(" + c.getRed() + ", " + c.getGreen() + ", " + c.getBlue() + ", " + a + ")";
    }

    private static String trimFloat(float f) {
        String s = String.format(Locale.ROOT, "%.3f", f);
        // strip trailing zeros
        while (s.contains(".") && (s.endsWith("0") || s.endsWith("."))) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
