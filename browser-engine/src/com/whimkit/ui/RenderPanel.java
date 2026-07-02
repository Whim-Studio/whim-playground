package com.whimkit.ui;

import com.whimkit.app.BrowserEngine;
import com.whimkit.app.Page;
import com.whimkit.dom.Element;
import com.whimkit.layout.LayoutBox;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;

/**
 * The page canvas. It lays out the current {@link Page} to its own width using
 * the engine's layout engine, paints the resulting {@link LayoutBox} tree with
 * the renderer, and translates mouse input into link navigation and DOM click
 * events.
 *
 * <p>The panel's preferred size is the full document height, so the enclosing
 * {@link javax.swing.JScrollPane} provides scrolling; the renderer honors the
 * exposed clip, so only the visible region repaints.</p>
 */
public final class RenderPanel extends JPanel implements Scrollable {

    private final BrowserEngine engine;
    private final BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    private Page page;
    private LayoutBox root;
    private int lastWidth = -1;

    /** Called when the user clicks a hyperlink. */
    public interface LinkListener { void onLink(String href); }
    /** Called when the pointer moves over content (href or null) for the status bar. */
    public interface HoverListener { void onHover(String href); }

    private LinkListener linkListener;
    private HoverListener hoverListener;
    private ClickDispatcher clickDispatcher;

    /** Lets the tab route DOM click events into the JS runtime. */
    public interface ClickDispatcher { void dispatch(Element target); }

    public RenderPanel(BrowserEngine engine) {
        this.engine = engine;
        setBackground(Color.WHITE);
        setOpaque(true);
        setFocusable(true);
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                if (getWidth() != lastWidth) relayout();
            }
        });
        Mouse mouse = new Mouse();
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    public void setLinkListener(LinkListener l) { this.linkListener = l; }
    public void setHoverListener(HoverListener l) { this.hoverListener = l; }
    public void setClickDispatcher(ClickDispatcher d) { this.clickDispatcher = d; }

    public void setPage(Page page) {
        this.page = page;
        relayout();
        revalidate();
        repaint();
    }

    /** Re-runs layout at the current width (after resize, or a JS-triggered reflow). */
    public void relayout() {
        int w = getWidth();
        if (w <= 0) w = 1000;
        lastWidth = w;
        if (page == null || page.document == null) { root = null; return; }
        Graphics2D g = scratch.createGraphics();
        try {
            root = engine.subsystems().layout.layout(page.document, w, g);
            page.layoutRoot = root;
        } catch (RuntimeException ex) {
            System.err.println("[render-panel] layout failed: " + ex);
            root = null;
        } finally {
            g.dispose();
        }
        int h = (root != null) ? Math.max(1, Math.round(root.marginBottomEdge())) : 1;
        setPreferredSize(new Dimension(w, h));
        revalidate();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (root != null) {
            engine.subsystems().renderer.paint(root, (Graphics2D) g);
        }
    }

    private final class Mouse extends java.awt.event.MouseAdapter {
        @Override public void mousePressed(java.awt.event.MouseEvent e) {
            if (root == null) return;
            LayoutBox link = root.hitTestLink(e.getX(), e.getY());
            Element el = hitElement(root, e.getX(), e.getY());
            if (clickDispatcher != null && el != null) clickDispatcher.dispatch(el);
            if (link != null && link.href != null && linkListener != null) {
                linkListener.onLink(resolve(link.href));
            }
        }
        @Override public void mouseMoved(java.awt.event.MouseEvent e) {
            if (root == null) return;
            LayoutBox link = root.hitTestLink(e.getX(), e.getY());
            setCursor(Cursor.getPredefinedCursor(link != null
                    ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            if (hoverListener != null) hoverListener.onHover(link != null ? resolve(link.href) : null);
        }
    }

    private String resolve(String href) {
        String base = (page != null && page.document != null) ? page.document.getBaseUri() : "";
        return com.whimkit.net.Url.resolve(base, href);
    }

    /** Finds the deepest element box containing the point (for JS click dispatch). */
    private static Element hitElement(LayoutBox box, float x, float y) {
        Element found = null;
        com.whimkit.layout.Rect b = box.dimensions.borderBox();
        if (x >= b.x && x <= b.right() && y >= b.y && y <= b.bottom() && box.element != null) {
            found = box.element;
        }
        for (LayoutBox c : box.children) {
            Element deeper = hitElement(c, x, y);
            if (deeper != null) found = deeper;
        }
        return found;
    }

    // --- Scrollable: sensible wheel/scrollbar increments, track viewport width ---
    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
        return o == SwingUtilities.VERTICAL ? r.height - 24 : r.width - 24;
    }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
}
