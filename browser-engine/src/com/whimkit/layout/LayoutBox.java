package com.whimkit.layout;

import com.whimkit.css.ComputedStyle;
import com.whimkit.dom.Element;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

/**
 * A node in the layout tree — the bridge structure between the LAYOUT engine
 * (which builds and positions these) and the RENDER engine (which walks them and
 * paints). It deliberately lives in the foundation so both subsystems compile
 * against it without depending on each other.
 *
 * <p>A layout tree is <em>not</em> the DOM tree: elements with
 * {@code display:none} produce no box, inline content is wrapped in anonymous
 * boxes, and text runs become {@link BoxType#TEXT} boxes. Each box keeps a
 * back-pointer to its source {@link Element} (nullable for anonymous/text boxes)
 * and the {@link ComputedStyle} that governs it.</p>
 */
public class LayoutBox {

    /** Box generation kinds understood by the layout and paint stages. */
    public enum BoxType {
        BLOCK,        // block-level box establishing/participating in a block context
        INLINE,       // inline-level box
        INLINE_BLOCK, // atomic inline that lays out its contents as a block
        ANONYMOUS,    // synthesized wrapper (e.g. block wrapping inline runs)
        TEXT,         // a run of text
        LIST_ITEM     // block box that also paints a marker
    }

    public BoxType type;
    public final Dimensions dimensions = new Dimensions();
    public final List<LayoutBox> children = new ArrayList<LayoutBox>();

    /** Source element; {@code null} for anonymous and text boxes. */
    public Element element;
    /** Governing style; never {@code null} for boxes that paint. */
    public ComputedStyle style;

    /** Non-null only for {@link BoxType#TEXT} boxes: the (whitespace-processed) run. */
    public String text;

    /** Non-null only for replaced {@code <img>} boxes that finished loading. */
    public Image image;

    /** For link hit-testing: the resolved href if this box originates from an {@code <a>}. */
    public String href;

    public LayoutBox(BoxType type, Element element, ComputedStyle style) {
        this.type = type;
        this.element = element;
        this.style = style;
    }

    public static LayoutBox textBox(String text, ComputedStyle style) {
        LayoutBox b = new LayoutBox(BoxType.TEXT, null, style);
        b.text = text;
        return b;
    }

    public LayoutBox addChild(LayoutBox child) {
        children.add(child);
        return child;
    }

    /**
     * Depth-first hit test: returns the deepest box whose border box contains
     * the point and that carries a non-null {@link #href}. Used by the UI to
     * turn clicks into navigations.
     *
     * @return the innermost hyperlink box under the point, or {@code null}.
     */
    public LayoutBox hitTestLink(float px, float py) {
        LayoutBox found = null;
        Rect b = dimensions.borderBox();
        boolean inside = px >= b.x && px <= b.right() && py >= b.y && py <= b.bottom();
        if (inside && href != null) {
            found = this;
        }
        for (LayoutBox c : children) {
            LayoutBox deeper = c.hitTestLink(px, py);
            if (deeper != null) found = deeper;
        }
        return found;
    }

    /** @return the bottom edge of this box's margin area, for total document height. */
    public float marginBottomEdge() {
        return dimensions.marginBox().bottom();
    }
}
