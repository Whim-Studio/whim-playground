package com.whimkit.app;

import com.whimkit.dom.Document;
import com.whimkit.dom.Element;
import com.whimkit.net.Url;
import com.whimkit.net.WebResponse;
import com.whimkit.render.ImageStore;

import javax.swing.SwingWorker;
import java.util.List;

/**
 * The engine coordinator: it drives the load pipeline
 * (network → HTML parse → CSS collect+cascade) off the EDT and hands a prepared
 * {@link Page} back to the UI. Layout and paint are performed by the render panel
 * (they need a live {@link java.awt.Graphics2D} and the viewport width).
 *
 * <p>Threading: {@link #load} runs the blocking work on a {@link SwingWorker}
 * background thread and invokes the callback on the EDT via {@code done()}. The
 * produced {@link Document} is thereafter mutated only on the EDT.</p>
 */
public final class BrowserEngine {

    /** EDT callback for a completed (or failed) load. */
    public interface PageCallback {
        void onLoaded(Page page);
    }

    private final Subsystems sub;

    public BrowserEngine(Subsystems sub) {
        this.sub = sub;
    }

    public Subsystems subsystems() {
        return sub;
    }

    public ImageStore imageStore() {
        return sub.renderer.getImageStore();
    }

    /** Loads {@code url} asynchronously; {@code cb} is invoked on the EDT. */
    public void load(final String url, final PageCallback cb) {
        new SwingWorker<Page, Void>() {
            @Override
            protected Page doInBackground() {
                return fetchAndPrepare(url);
            }
            @Override
            protected void done() {
                Page page;
                try {
                    page = get();
                } catch (Exception e) {
                    page = new Page(url, url, null, null, "load interrupted: " + e);
                }
                if (cb != null) cb.onLoaded(page);
            }
        }.execute();
    }

    /** The blocking pipeline; runs on a worker thread. Never throws. */
    private Page fetchAndPrepare(String url) {
        try {
            WebResponse resp = sub.loader.load(url);
            if (resp == null || !resp.isOk()) {
                String msg = resp == null ? "no response" : errorMessage(resp);
                return new Page(url, url, errorDocument(url, msg), resp, null);
            }
            if (!resp.isHtml()) {
                // Non-HTML: show a simple wrapper (images handled by the img flow elsewhere).
                Document d = errorDocument(resp.getFinalUrl(),
                        "Content-Type " + resp.getContentType() + " is not rendered as a page.");
                return new Page(url, resp.getFinalUrl(), d, resp, null);
            }
            String html = resp.getText();
            String base = resp.getFinalUrl();
            Document doc = sub.parser.parse(html, base);
            doc.setBaseUri(base);

            // Pre-fetch external <script src> bodies so the JS runtime can run them.
            for (Element script : doc.getElementsByTagName("script")) {
                String src = script.getAttribute("src");
                if (src != null && !src.isEmpty() && script.getTextContent().trim().isEmpty()) {
                    WebResponse sr = sub.loader.load(Url.resolve(base, src));
                    if (sr != null && sr.isOk()) {
                        script.appendChild(doc.createTextNode(sr.getText()));
                    }
                }
            }

            // Collect author CSS: inline <style> + linked <link rel=stylesheet>.
            sub.styles.reset();
            for (Element link : doc.getElementsByTagName("link")) {
                String rel = link.getAttribute("rel");
                String href = link.getAttribute("href");
                if (rel != null && rel.toLowerCase().contains("stylesheet") && href != null && !href.isEmpty()) {
                    WebResponse cssResp = sub.loader.load(Url.resolve(base, href));
                    if (cssResp != null && cssResp.isOk()) sub.styles.addAuthorCss(cssResp.getText());
                }
            }
            for (Element style : doc.getElementsByTagName("style")) {
                String css = style.getTextContent();
                if (css != null && !css.isEmpty()) sub.styles.addAuthorCss(css);
            }
            sub.styles.styleDocument(doc);

            return new Page(url, base, doc, resp, null);
        } catch (RuntimeException ex) {
            return new Page(url, url, errorDocument(url, "engine error: " + ex), null, null);
        }
    }

    private static String errorMessage(WebResponse resp) {
        if (resp.getError() != null) return resp.getError();
        return "HTTP " + resp.getStatusCode();
    }

    /** Builds a small synthetic error/status document so the UI always has something to show. */
    private Document errorDocument(String url, String message) {
        String html = "<html><head><title>WhimKit</title></head><body>"
                + "<h1>Unable to display page</h1>"
                + "<p><b>" + escape(url) + "</b></p>"
                + "<p>" + escape(message) + "</p>"
                + "<hr><p>WhimKit renders a pragmatic subset of the web; some resources and "
                + "scripts are unsupported by design.</p></body></html>";
        Document d = sub.parser.parse(html, url);
        sub.styles.reset();
        sub.styles.styleDocument(d);
        return d;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Loads {@code <img>} resources for a laid-out document into the shared image
     * store, invoking {@code onImage} on the EDT after each successful decode so
     * the UI can repaint. Runs its fetching on a background worker.
     */
    public void loadImages(final Document doc, final Runnable onImage) {
        if (doc == null) return;
        final List<Element> imgs = doc.getElementsByTagName("img");
        if (imgs.isEmpty()) return;
        final String base = doc.getBaseUri();
        new SwingWorker<Void, Element>() {
            @Override
            protected Void doInBackground() {
                for (Element img : imgs) {
                    String src = img.getAttribute("src");
                    if (src == null || src.isEmpty()) continue;
                    String abs = Url.resolve(base, src);
                    if (imageStore().get(src) != null) continue;
                    WebResponse r = sub.loader.load(abs);
                    if (r != null && r.isOk() && r.isImage()) {
                        if (imageStore().decode(src, r.getBody()) != null) publish(img);
                    }
                }
                return null;
            }
            @Override
            protected void process(List<Element> chunks) {
                if (onImage != null) onImage.run();
            }
        }.execute();
    }
}
