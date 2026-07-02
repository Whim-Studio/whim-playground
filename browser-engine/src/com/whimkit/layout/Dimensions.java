package com.whimkit.layout;

/**
 * The computed geometry of a box: its content rectangle plus the padding,
 * border, and margin rings around it (the CSS box model).
 *
 * <p>The content {@link Rect} is in absolute document coordinates once layout
 * has run. The three edge rings let the renderer reconstruct the padding, border,
 * and margin boxes without re-deriving them.</p>
 */
public class Dimensions {
    public final Rect content = new Rect();
    public final EdgeSizes padding = new EdgeSizes();
    public final EdgeSizes border = new EdgeSizes();
    public final EdgeSizes margin = new EdgeSizes();

    /** @return content box grown by padding. */
    public Rect paddingBox() {
        return content.expandedBy(padding);
    }

    /** @return padding box grown by border widths (the painted border rectangle). */
    public Rect borderBox() {
        return paddingBox().expandedBy(border);
    }

    /** @return border box grown by margins (the space the box reserves in flow). */
    public Rect marginBox() {
        return borderBox().expandedBy(margin);
    }
}
