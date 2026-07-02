package com.whimkit.layout.engine;

import com.whimkit.css.ComputedStyle;
import com.whimkit.dom.Document;
import com.whimkit.dom.Element;
import com.whimkit.dom.Node;
import com.whimkit.dom.TextNode;
import com.whimkit.layout.Dimensions;
import com.whimkit.layout.LayoutBox;
import com.whimkit.layout.LayoutBox.BoxType;
import com.whimkit.layout.LayoutEngine;
import com.whimkit.layout.Rect;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A normal-flow layout engine implementing {@link LayoutEngine}.
 *
 * <p>It produces a positioned {@link LayoutBox} tree from a styled
 * {@link Document}. Two formatting contexts are handled:</p>
 * <ul>
 *   <li><b>Block context</b> — a box whose element has any block-level element
 *       child. Children are stacked vertically; runs of inline/text content
 *       between block children are wrapped in synthesized {@link BoxType#ANONYMOUS}
 *       boxes (matching real browser box generation).</li>
 *   <li><b>Inline context</b> — a box with only inline/text content. Its inline
 *       descendants are flattened into styled runs, split into words, and packed
 *       into line boxes broken at the available width using {@link FontMetrics}.
 *       {@code text-align} shifts each completed line.</li>
 * </ul>
 *
 * <p>Coordinates are absolute (document space, origin top-left, y downward).
 * Layout runs on the EDT; the supplied {@link Graphics2D} is used only for font
 * metrics and is never painted to.</p>
 *
 * <p><b>Scope (documented gaps):</b> normal flow + basic {@code text-align} +
 * inline images. Nested inline box backgrounds/borders are flattened to their
 * text runs; {@code position:absolute/fixed}, real floats, flexbox, and grid are
 * out of scope.</p>
 */
public final class BlockLayoutEngine implements LayoutEngine {

    /** A flattened piece of inline content: a text run, an inline image, or a hard break. */
    private static final class Run {
        final String text;          // non-null for text runs
        final Element replaced;     // non-null for <img>
        final boolean lineBreak;    // true for <br>
        final ComputedStyle style;
        final String href;
        Run(String text, Element replaced, boolean lineBreak, ComputedStyle style, String href) {
            this.text = text; this.replaced = replaced; this.lineBreak = lineBreak;
            this.style = style; this.href = href;
        }
    }

    @Override
    public LayoutBox layout(Document doc, float viewportWidth, Graphics2D g) {
        Element root = doc == null ? null : doc.getBody();
        if (root == null && doc != null) root = doc.getDocumentElement();
        ComputedStyle rootStyle = (root != null && root.getComputedStyle() != null)
                ? root.getComputedStyle() : ComputedStyle.initial();
        LayoutBox rootBox = new LayoutBox(BoxType.BLOCK, root, rootStyle);
        if (root == null) {
            rootBox.dimensions.content.width = viewportWidth;
            return rootBox;
        }
        try {
            layoutBlock(rootBox, viewportWidth, 0f, 0f, g);
        } catch (RuntimeException ex) {
            // Layout must never crash the browser; degrade to whatever was positioned.
            System.err.println("[layout] recovered from: " + ex);
        }
        return rootBox;
    }

    // --- block layout ------------------------------------------------------

    /** Sizes and positions {@code box} and all of its descendants. */
    private void layoutBlock(LayoutBox box, float cbWidth, float startX, float startY, Graphics2D g) {
        ComputedStyle s = box.style;
        Dimensions d = box.dimensions;

        d.margin.top = s.marginTop; d.margin.right = s.marginRight;
        d.margin.bottom = s.marginBottom; d.margin.left = s.marginLeft;
        d.border.top = s.borderTopWidth; d.border.right = s.borderRightWidth;
        d.border.bottom = s.borderBottomWidth; d.border.left = s.borderLeftWidth;
        d.padding.top = s.paddingTop; d.padding.right = s.paddingRight;
        d.padding.bottom = s.paddingBottom; d.padding.left = s.paddingLeft;

        float horizExtras = d.margin.left + d.margin.right + d.border.left + d.border.right
                + d.padding.left + d.padding.right;
        float contentWidth = (s.width != null) ? s.width : (cbWidth - horizExtras);
        if (contentWidth < 0) contentWidth = 0;
        d.content.width = contentWidth;
        d.content.x = startX + d.margin.left + d.border.left + d.padding.left;
        d.content.y = startY + d.margin.top + d.border.top + d.padding.top;

        float contentBottom;
        if (box.element != null && hasBlockChildren(box.element)) {
            contentBottom = layoutBlockChildren(box, d.content.x, d.content.y, contentWidth, g);
        } else {
            List<Node> kids = (box.element != null)
                    ? box.element.getChildNodes() : new ArrayList<Node>();
            float h = layoutInline(box, kids, box.style, contentWidth, d.content.x, d.content.y, g);
            contentBottom = d.content.y + h;
        }

        d.content.height = (s.height != null) ? s.height : (contentBottom - d.content.y);
    }

    /** Lays out block children in vertical flow, wrapping inline runs in anonymous boxes. */
    private float layoutBlockChildren(LayoutBox parent, float contentX, float startY,
                                      float contentWidth, Graphics2D g) {
        float y = startY;
        List<Node> pendingInline = new ArrayList<Node>();
        for (Node kid : parent.element.getChildNodes()) {
            if (isBlockLevel(kid)) {
                y = flushInline(parent, pendingInline, contentX, y, contentWidth, g);
                pendingInline.clear();
                Element ce = (Element) kid;
                ComputedStyle cs = ce.getComputedStyle();
                if (cs == null || cs.isNone()) continue;
                BoxType t = isListItem(ce, cs) ? BoxType.LIST_ITEM : BoxType.BLOCK;
                LayoutBox cb = new LayoutBox(t, ce, cs);
                layoutBlock(cb, contentWidth, contentX, y, g);
                parent.addChild(cb);
                y += cb.dimensions.marginBox().height;
            } else if (isRenderableInline(kid)) {
                pendingInline.add(kid);
            }
        }
        y = flushInline(parent, pendingInline, contentX, y, contentWidth, g);
        return y;
    }

    private float flushInline(LayoutBox parent, List<Node> inlineNodes, float contentX,
                              float y, float contentWidth, Graphics2D g) {
        if (inlineNodes.isEmpty()) return y;
        LayoutBox anon = new LayoutBox(BoxType.ANONYMOUS, null, parent.style);
        anon.dimensions.content.x = contentX;
        anon.dimensions.content.y = y;
        anon.dimensions.content.width = contentWidth;
        List<Node> copy = new ArrayList<Node>(inlineNodes);
        float h = layoutInline(anon, copy, parent.style, contentWidth, contentX, y, g);
        anon.dimensions.content.height = h;
        parent.addChild(anon);
        return y + h;
    }

    // --- inline layout -----------------------------------------------------

    /**
     * Flattens {@code nodes} into styled runs and packs them into line boxes.
     *
     * @return the total height consumed by the produced lines.
     */
    private float layoutInline(LayoutBox container, List<Node> nodes, ComputedStyle base,
                               float availWidth, float originX, float originY, Graphics2D g) {
        List<Run> runs = new ArrayList<Run>();
        for (Node n : nodes) collectInline(n, base, null, runs);
        if (runs.isEmpty()) return 0f;

        float x = originX;
        float y = originY;
        float maxRight = originX + availWidth;
        List<LayoutBox> line = new ArrayList<LayoutBox>();
        float lineHeight = 0f;

        for (Run run : runs) {
            if (run.lineBreak) {
                lineHeight = Math.max(lineHeight, run.style.lineHeight);
                alignLine(line, base.textAlign, originX, availWidth, x);
                line.clear();
                x = originX;
                y += lineHeight;
                lineHeight = 0f;
                continue;
            }
            if (run.replaced != null) {
                float[] wh = replacedSize(run.replaced);
                if (x + wh[0] > maxRight && x > originX) {
                    alignLine(line, base.textAlign, originX, availWidth, x);
                    line.clear(); x = originX; y += lineHeight; lineHeight = 0f;
                }
                LayoutBox img = new LayoutBox(BoxType.INLINE_BLOCK, run.replaced, run.style);
                img.href = run.href;
                img.dimensions.content.x = x;
                img.dimensions.content.y = y;
                img.dimensions.content.width = wh[0];
                img.dimensions.content.height = wh[1];
                container.addChild(img);
                line.add(img);
                x += wh[0];
                lineHeight = Math.max(lineHeight, Math.max(wh[1], run.style.lineHeight));
                continue;
            }
            // text run
            Font font = Fonts.forStyle(run.style);
            FontMetrics fm = g.getFontMetrics(font);
            int spaceW = fm.charWidth(' ');
            int textH = fm.getHeight();
            float runLine = Math.max(run.style.lineHeight, textH);
            boolean pre = "pre".equals(run.style.whiteSpace);
            List<String> words = splitWords(run.text, pre);
            for (String word : words) {
                if (word.equals("\n")) { // preserved newline in pre
                    alignLine(line, base.textAlign, originX, availWidth, x);
                    line.clear(); x = originX; y += Math.max(lineHeight, runLine); lineHeight = 0f;
                    continue;
                }
                int wWidth = fm.stringWidth(word);
                boolean leadingSpace = !line.isEmpty();
                float advance = (leadingSpace ? spaceW : 0) + wWidth;
                if (!pre && x + advance > maxRight && !line.isEmpty()) {
                    alignLine(line, base.textAlign, originX, availWidth, x);
                    line.clear(); x = originX; y += lineHeight; lineHeight = 0f;
                    leadingSpace = false;
                }
                if (leadingSpace) x += spaceW;
                LayoutBox tb = LayoutBox.textBox(word, run.style);
                tb.href = run.href;
                tb.dimensions.content.x = x;
                tb.dimensions.content.y = y;
                tb.dimensions.content.width = wWidth;
                tb.dimensions.content.height = textH;
                container.addChild(tb);
                line.add(tb);
                x += wWidth;
                lineHeight = Math.max(lineHeight, runLine);
            }
        }
        if (!line.isEmpty()) {
            alignLine(line, base.textAlign, originX, availWidth, x);
            y += lineHeight;
        }
        return y - originY;
    }

    /** Shifts a completed line's boxes for {@code center}/{@code right} alignment. */
    private void alignLine(List<LayoutBox> line, String align, float originX,
                           float availWidth, float lineEndX) {
        if (line.isEmpty() || align == null || "left".equals(align) || "justify".equals(align)) return;
        float used = lineEndX - originX;
        float free = availWidth - used;
        if (free <= 0) return;
        float shift = "center".equals(align) ? free / 2f : free; // right
        for (LayoutBox b : line) b.dimensions.content.x += shift;
    }

    /** Recursively collects inline content, propagating style and hyperlink context. */
    private void collectInline(Node node, ComputedStyle parentStyle, String href, List<Run> out) {
        if (node instanceof TextNode) {
            String data = ((TextNode) node).getData();
            if (data != null && !data.isEmpty()) out.add(new Run(data, null, false, parentStyle, href));
            return;
        }
        if (!(node instanceof Element)) return;
        Element e = (Element) node;
        ComputedStyle s = e.getComputedStyle() != null ? e.getComputedStyle() : parentStyle;
        if (s.isNone()) return;
        String tag = e.getTagName();
        String h = href;
        if ("a".equals(tag) && e.getAttribute("href") != null) h = e.getAttribute("href");
        if ("br".equals(tag)) { out.add(new Run(null, null, true, s, h)); return; }
        if ("img".equals(tag)) { out.add(new Run(null, e, false, s, h)); return; }
        for (Node c : e.getChildNodes()) collectInline(c, s, h, out);
    }

    // --- helpers -----------------------------------------------------------

    private static boolean hasBlockChildren(Element e) {
        for (Node c : e.getChildNodes()) {
            if (isBlockLevel(c)) return true;
        }
        return false;
    }

    private static boolean isBlockLevel(Node n) {
        if (!(n instanceof Element)) return false;
        ComputedStyle cs = ((Element) n).getComputedStyle();
        return cs != null && !cs.isNone() && cs.isBlockLevel();
    }

    private static boolean isListItem(Element e, ComputedStyle cs) {
        return "list-item".equals(cs.display) || "li".equals(e.getTagName());
    }

    private static boolean isRenderableInline(Node n) {
        if (n instanceof TextNode) return true;
        if (n instanceof Element) {
            ComputedStyle cs = ((Element) n).getComputedStyle();
            return cs != null && !cs.isNone();
        }
        return false;
    }

    /** Intrinsic size for a replaced {@code <img>} from its width/height attributes. */
    private static float[] replacedSize(Element img) {
        float w = parseDim(img.getAttribute("width"), 0);
        float h = parseDim(img.getAttribute("height"), 0);
        if (w <= 0) w = (h > 0 ? h : 32);
        if (h <= 0) h = (w > 0 ? w : 32);
        return new float[]{w, h};
    }

    private static float parseDim(String v, float def) {
        if (v == null) return def;
        try {
            return Float.parseFloat(v.toLowerCase(Locale.ROOT).replace("px", "").trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * Splits a text run into layout tokens. For {@code white-space:normal},
     * collapses all whitespace and returns bare words. For {@code pre}, returns
     * words plus explicit {@code "\n"} tokens marking preserved line breaks.
     */
    private static List<String> splitWords(String text, boolean pre) {
        List<String> out = new ArrayList<String>();
        if (text == null) return out;
        if (pre) {
            String[] lines = text.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                for (String w : lines[i].split(" ")) if (!w.isEmpty()) out.add(w);
                if (i < lines.length - 1) out.add("\n");
            }
            return out;
        }
        for (String w : text.trim().split("\\s+")) {
            if (!w.isEmpty()) out.add(w);
        }
        // Preserve a single leading/trailing space semantics loosely: normal collapse is fine.
        return out;
    }
}
