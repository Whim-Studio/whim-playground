package com.whimkit.render;

import com.whimkit.css.ComputedStyle;
import com.whimkit.layout.LayoutBox;
import com.whimkit.layout.Rect;
import com.whimkit.layout.engine.Fonts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;

/**
 * A software renderer that paints a positioned {@link LayoutBox} tree with Java2D
 * ({@link Renderer} implementation).
 *
 * <p>Paint order per box follows CSS: background fill (padding box) → borders →
 * content (text / image / list marker) → children. Painting is a pure function
 * of the layout tree, so it can be repeated freely for scrolling and resizing.
 * The renderer honors the clip already installed on the {@link Graphics2D}, and
 * additionally culls boxes whose margin box falls entirely outside the clip — a
 * viewport optimization (DESIGN.md §14).</p>
 *
 * <p>Images are looked up in a shared {@link ImageStore} by their resolved
 * {@code src}; when absent (not yet loaded or unsupported format) a labelled
 * placeholder is drawn so layout stays stable.</p>
 */
public final class Java2DRenderer implements Renderer {

    private final ImageStore images;

    public Java2DRenderer() {
        this(new ImageStore());
    }

    public Java2DRenderer(ImageStore images) {
        this.images = images == null ? new ImageStore() : images;
    }

    public ImageStore getImageStore() {
        return images;
    }

    @Override
    public void paint(LayoutBox root, Graphics2D g) {
        if (root == null || g == null) return;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        java.awt.Rectangle clip = g.getClipBounds();
        paintBox(root, g, clip);
    }

    private void paintBox(LayoutBox box, Graphics2D g, java.awt.Rectangle clip) {
        ComputedStyle s = box.style;
        if (s != null && "hidden".equals(s.visibility)) {
            // Hidden boxes reserve space but paint nothing; children may still be visible.
            for (LayoutBox c : box.children) paintBox(c, g, clip);
            return;
        }
        Rect margin = box.dimensions.marginBox();
        // Viewport culling: skip subtree entirely outside the dirty region.
        if (clip != null && (margin.bottom() < clip.y || margin.y > clip.y + clip.height
                || margin.right() < clip.x || margin.x > clip.x + clip.width)) {
            return;
        }

        paintBackground(box, g);
        paintBorders(box, g);

        switch (box.type) {
            case TEXT:
                paintText(box, g);
                break;
            case INLINE_BLOCK:
                paintReplaced(box, g);
                break;
            case LIST_ITEM:
                paintListMarker(box, g);
                break;
            default:
                break;
        }
        for (LayoutBox c : box.children) paintBox(c, g, clip);
    }

    private void paintBackground(LayoutBox box, Graphics2D g) {
        if (box.style == null || box.style.backgroundColor == null) return;
        Rect p = box.dimensions.paddingBox();
        g.setColor(box.style.backgroundColor);
        g.fillRect(Math.round(p.x), Math.round(p.y), Math.round(p.width), Math.round(p.height));
    }

    private void paintBorders(LayoutBox box, Graphics2D g) {
        ComputedStyle s = box.style;
        if (s == null) return;
        Rect b = box.dimensions.borderBox();
        Color bc = s.borderColor != null ? s.borderColor : s.color;
        Stroke old = g.getStroke();
        g.setColor(bc);
        if (s.borderTopWidth > 0) fillEdge(g, b.x, b.y, b.width, s.borderTopWidth);
        if (s.borderBottomWidth > 0)
            fillEdge(g, b.x, b.bottom() - s.borderBottomWidth, b.width, s.borderBottomWidth);
        if (s.borderLeftWidth > 0) fillEdge(g, b.x, b.y, s.borderLeftWidth, b.height);
        if (s.borderRightWidth > 0)
            fillEdge(g, b.right() - s.borderRightWidth, b.y, s.borderRightWidth, b.height);
        g.setStroke(old);
    }

    private void fillEdge(Graphics2D g, float x, float y, float w, float h) {
        g.fillRect(Math.round(x), Math.round(y), Math.max(1, Math.round(w)), Math.max(1, Math.round(h)));
    }

    private void paintText(LayoutBox box, Graphics2D g) {
        if (box.text == null || box.text.isEmpty() || box.style == null) return;
        Font font = Fonts.forStyle(box.style);
        g.setFont(font);
        g.setColor(box.style.color != null ? box.style.color : Color.BLACK);
        FontMetrics fm = g.getFontMetrics(font);
        Rect c = box.dimensions.content;
        int baseline = Math.round(c.y) + fm.getAscent();
        g.drawString(box.text, Math.round(c.x), baseline);
        if (box.style.underline) {
            int uy = baseline + 1;
            g.fillRect(Math.round(c.x), uy, fm.stringWidth(box.text), Math.max(1, Math.round(box.style.fontSize / 14f)));
        }
        if (box.style.lineThrough) {
            int ly = Math.round(c.y) + fm.getAscent() - fm.getAscent() / 3;
            g.fillRect(Math.round(c.x), ly, fm.stringWidth(box.text), 1);
        }
    }

    private void paintReplaced(LayoutBox box, Graphics2D g) {
        Rect c = box.dimensions.content;
        int x = Math.round(c.x), y = Math.round(c.y), w = Math.round(c.width), h = Math.round(c.height);
        Image img = box.image;
        if (img == null && box.element != null) {
            img = images.get(box.element.getAttribute("src"));
        }
        if (img != null) {
            g.drawImage(img, x, y, w, h, null);
            return;
        }
        // Placeholder frame with alt text.
        g.setColor(new Color(0xF0F0F0));
        g.fillRect(x, y, w, h);
        g.setColor(new Color(0xB0B0B0));
        g.drawRect(x, y, Math.max(0, w - 1), Math.max(0, h - 1));
        String alt = box.element != null ? box.element.getAttribute("alt") : null;
        if (alt != null && !alt.isEmpty() && w > 20 && h > 12) {
            g.setColor(Color.DARK_GRAY);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            Shape oldClip = g.getClip();
            g.clipRect(x, y, w, h);
            g.drawString(alt, x + 3, y + 13);
            g.setClip(oldClip);
        }
    }

    private void paintListMarker(LayoutBox box, Graphics2D g) {
        if (box.style == null) return;
        Rect c = box.dimensions.content;
        g.setColor(box.style.color != null ? box.style.color : Color.BLACK);
        int size = Math.max(4, Math.round(box.style.fontSize * 0.35f));
        int cx = Math.round(c.x) - 14;
        int cy = Math.round(c.y) + Math.round(box.style.fontSize * 0.5f);
        String type = box.style.listStyleType;
        if ("none".equals(type)) return;
        if ("square".equals(type)) {
            g.fillRect(cx, cy - size / 2, size, size);
        } else if ("circle".equals(type)) {
            g.drawOval(cx, cy - size / 2, size, size);
        } else { // disc / default
            g.fillOval(cx, cy - size / 2, size, size);
        }
    }
}
