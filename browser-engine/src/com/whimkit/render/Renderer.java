package com.whimkit.render;

import com.whimkit.layout.LayoutBox;

import java.awt.Graphics2D;

/**
 * Contract for painting a positioned {@link LayoutBox} tree with Java2D.
 *
 * <p>Implemented by {@code com.whimkit.render.Java2DRenderer}. The renderer walks
 * the tree in paint order (backgrounds/borders, then content, then children),
 * honoring the clip already installed on the supplied {@link Graphics2D} so the
 * UI can restrict painting to the dirty viewport region.</p>
 */
public interface Renderer {

    /**
     * @param root the positioned root layout box (may be {@code null} → paint nothing).
     * @param g    the target graphics; its current clip bounds the paint region.
     */
    void paint(LayoutBox root, Graphics2D g);
}
