package com.whimkit.layout;

import com.whimkit.dom.Document;

import java.awt.Graphics2D;

/**
 * Contract for building and positioning the {@link LayoutBox} tree from a styled
 * {@link Document}.
 *
 * <p>Implemented by {@code com.whimkit.layout.engine.BlockLayoutEngine}. Every
 * element must already carry a {@link com.whimkit.css.ComputedStyle} (i.e. the
 * CSS {@link com.whimkit.css.StyleEngine} has run) before layout.</p>
 */
public interface LayoutEngine {

    /**
     * @param doc            a document whose elements are already styled.
     * @param viewportWidth  the available content width in CSS px.
     * @param measureContext a {@link Graphics2D} used solely for font metrics
     *                       (text measurement); it is not painted to. The caller
     *                       typically passes the graphics of a scratch image.
     * @return the root layout box, fully positioned in document coordinates.
     */
    LayoutBox layout(Document doc, float viewportWidth, Graphics2D measureContext);
}
