package com.whim.browser.app;

import com.whim.browser.ui.BrowserFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point for the Whim Browser — a minimal desktop web browser
 * built entirely on Java 8 + Swing/AWT with zero external dependencies.
 *
 * <p>The concerns stay cleanly separated:</p>
 * <ul>
 *   <li>{@code com.whim.browser.model}  — data types (WebResponse, Tab, history)</li>
 *   <li>{@code com.whim.browser.engine} — networking + HTML fetch engine</li>
 *   <li>{@code com.whim.browser.ui}     — the Swing presentation layer</li>
 *   <li>{@code com.whim.browser.app}    — this bootstrap</li>
 * </ul>
 *
 * <p><b>Renderer note.</b> Swing's {@link javax.swing.JEditorPane} uses an
 * HTML 3.2 rendering engine with no JavaScript and no HTML5/video support, so
 * only basic HTML pages display correctly. Java 8 only — no {@code var}, text
 * blocks, or later language features.</p>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // Fall back to the default cross-platform look and feel.
                }

                BrowserFrame frame = new BrowserFrame();
                frame.showWelcomePage(welcomeHtml());
                frame.setVisible(true);
            }
        });
    }

    /**
     * A built-in {@code about:}-style welcome page describing what the browser
     * can and cannot do. Kept to plain HTML 3.2 so it renders correctly in the
     * very engine it is describing.
     */
    private static String welcomeHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><title>Whim Browser</title></head>");
        sb.append("<body style=\"font-family:sans-serif; margin:28px;\">");
        sb.append("<h1>Whim Browser</h1>");
        sb.append("<p>A tiny web browser written in pure Java 8 Swing — no external "
                + "libraries, no embedded browser engine.</p>");
        sb.append("<h2>What works</h2>");
        sb.append("<ul>");
        sb.append("<li>Fetching pages over HTTP/HTTPS via the address bar</li>");
        sb.append("<li>Rendering basic <b>HTML</b> (headings, paragraphs, lists, "
                + "links, tables, simple styling)</li>");
        sb.append("<li>Clicking links, plus Back / Forward / Refresh</li>");
        sb.append("</ul>");
        sb.append("<h2>What doesn't</h2>");
        sb.append("<ul>");
        sb.append("<li><b>JavaScript</b> — there is no JS engine; scripts are ignored</li>");
        sb.append("<li><b>HTML5 &amp; CSS3</b> — the renderer only understands HTML 3.2 "
                + "and a little CSS, so modern pages look plain or broken</li>");
        sb.append("<li><b>Video</b> — YouTube and other HTML5-video pages are opened in "
                + "your operating system's native browser instead</li>");
        sb.append("</ul>");
        sb.append("<p style=\"color:#888; margin-top:24px;\">Type a URL above and press "
                + "Enter to get started.</p>");
        sb.append("</body></html>");
        return sb.toString();
    }
}
