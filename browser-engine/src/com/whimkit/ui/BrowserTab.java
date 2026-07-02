package com.whimkit.ui;

import com.whimkit.app.BrowserEngine;
import com.whimkit.app.Page;
import com.whimkit.app.Subsystems;
import com.whimkit.dom.Element;
import com.whimkit.js.NashornRuntime;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.util.concurrent.Executor;

/**
 * One browsing context: its own {@link BrowserEngine}, {@link History},
 * {@link RenderPanel}, and (per page) a {@link NashornRuntime}. Owns the
 * navigate/back/forward/reload logic and the reflow loop that re-styles,
 * re-lays-out, and repaints after script mutates the DOM.
 */
public final class BrowserTab {

    private final BrowserFrame owner;
    private final BrowserEngine engine;
    private final RenderPanel renderPanel;
    private final JScrollPane scroll;
    private final History history = new History();

    private Page page;
    private NashornRuntime js;
    private String currentUrl = "about:blank";
    private String title = "New Tab";
    private String status = "";

    private final Executor edt = new Executor() {
        public void execute(Runnable r) { SwingUtilities.invokeLater(r); }
    };

    public BrowserTab(BrowserFrame owner) {
        this.owner = owner;
        this.engine = new BrowserEngine(new Subsystems());
        this.renderPanel = new RenderPanel(engine);
        this.scroll = new JScrollPane(renderPanel);
        this.scroll.getVerticalScrollBar().setUnitIncrement(24);
        renderPanel.setLinkListener(new RenderPanel.LinkListener() {
            public void onLink(String href) { navigate(href, true); }
        });
        renderPanel.setHoverListener(new RenderPanel.HoverListener() {
            public void onHover(String href) { status = href == null ? "" : href; owner.updateChrome(BrowserTab.this); }
        });
        renderPanel.setClickDispatcher(new RenderPanel.ClickDispatcher() {
            public void dispatch(Element target) { if (js != null) js.dispatchEvent(target, "click"); }
        });
    }

    public JScrollPane getComponent() { return scroll; }
    public String getTitle() { return title; }
    public String getCurrentUrl() { return currentUrl; }
    public String getStatus() { return status; }
    public boolean canBack() { return history.canBack(); }
    public boolean canForward() { return history.canForward(); }
    public NashornRuntime getJs() { return js; }

    public void navigate(String url, final boolean addHistory) {
        if (url == null || url.trim().isEmpty()) return;
        final String target = normalize(url.trim());
        currentUrl = target;
        status = "Loading " + target + " ...";
        owner.updateChrome(this);
        engine.load(target, new BrowserEngine.PageCallback() {
            public void onLoaded(Page p) { onPage(p, addHistory); }
        });
    }

    private void onPage(Page p, boolean addHistory) {
        this.page = p;
        this.currentUrl = p.finalUrl != null ? p.finalUrl : currentUrl;
        this.title = p.getTitle();
        if (addHistory) history.push(currentUrl);
        renderPanel.setPage(p);
        status = "";
        owner.updateChrome(this);
        owner.refreshDevTools(this);

        // Load images (repaint as they arrive).
        if (p.document != null) {
            engine.loadImages(p.document, new Runnable() {
                public void run() { renderPanel.relayout(); renderPanel.revalidate(); renderPanel.repaint(); }
            });
        }
        runScripts();
    }

    /** Binds a fresh JS runtime to the page and runs its inline scripts. */
    private void runScripts() {
        if (js != null) js.dispose();
        js = new NashornRuntime();
        if (page == null || page.document == null) return;
        js.setUiExecutor(edt);
        js.setReflowListener(new Runnable() { public void run() { reflow(); } });
        js.bind(page.document);
        js.runInlineScripts();
    }

    /** Re-style + re-layout + repaint after a DOM mutation. */
    private void reflow() {
        if (page == null || page.document == null) return;
        try {
            engine.subsystems().styles.styleDocument(page.document);
        } catch (RuntimeException ex) {
            System.err.println("[tab] restyle failed: " + ex);
        }
        renderPanel.relayout();
        renderPanel.revalidate();
        renderPanel.repaint();
        title = page.getTitle();
        owner.updateChrome(this);
    }

    public void back() { String u = history.back(); if (u != null) navigate(u, false); }
    public void forward() { String u = history.forward(); if (u != null) navigate(u, false); }
    public void reload() { navigate(currentUrl, false); }

    public Page getPage() { return page; }

    /** Turns bare input like "example.com" or a search term into a fetchable URL. */
    private static String normalize(String input) {
        if (input.startsWith("about:") || input.startsWith("data:") || input.startsWith("file:")) return input;
        if (input.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) return input;
        if (input.contains(".") && !input.contains(" ")) return "https://" + input;
        // Fall back to a search query.
        try {
            return "https://duckduckgo.com/html/?q=" + java.net.URLEncoder.encode(input, "UTF-8");
        } catch (Exception e) {
            return "https://" + input;
        }
    }
}
