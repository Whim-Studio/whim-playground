package com.whimkit.app;

import com.whimkit.ui.BrowserFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.charset.Charset;
import java.util.Base64;

/**
 * Application entry point for the WhimKit browser.
 *
 * <p>Usage:</p>
 * <pre>
 *   java -cp out com.whimkit.app.Main            # opens the welcome page
 *   java -cp out com.whimkit.app.Main &lt;url&gt;      # opens the given URL
 * </pre>
 *
 * <p>The whole UI is created and driven on the Event Dispatch Thread.</p>
 */
public final class Main {

    private Main() {}

    public static void main(final String[] args) {
        final String start = (args.length > 0 && args[0] != null && !args[0].trim().isEmpty())
                ? args[0].trim() : welcomeUrl();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignore) { /* default L&F is fine */ }
                BrowserFrame frame = new BrowserFrame();
                frame.setVisible(true);
                frame.addTab(start);
            }
        });
    }

    /** A self-contained welcome page delivered as a {@code data:} URL. */
    private static String welcomeUrl() {
        String html =
            "<!DOCTYPE html><html><head><title>WhimKit</title>"
            + "<style>"
            + "body{font-family:sans-serif;margin:24px;color:#222}"
            + "h1{color:#5b2be0} .lead{font-size:18px;color:#444}"
            + ".card{background:#f4f1ff;border:1px solid #d9cffb;padding:16px;margin:16px 0}"
            + "a{color:#1a0dab} code{background:#eee;padding:1px 4px}"
            + "</style></head><body>"
            + "<h1>WhimKit Browser</h1>"
            + "<p class=\"lead\">A from-scratch web engine in <b>pure Java 8</b> — custom HTML parser, "
            + "DOM, CSS cascade, box-model layout, and a Java2D software renderer.</p>"
            + "<div class=\"card\"><h3>Try it</h3><ul>"
            + "<li>Type a URL in the address bar (e.g. <code>example.com</code>) and press Enter.</li>"
            + "<li>Click a link below to navigate.</li>"
            + "<li>Open <b>DevTools</b> to inspect the DOM and run JavaScript.</li>"
            + "</ul></div>"
            + "<p>Sample links: <a href=\"https://example.com\">example.com</a> &nbsp; "
            + "<a href=\"https://www.iana.org/domains/reserved\">IANA reserved domains</a></p>"
            + "<p id=\"js\">JavaScript status: checking…</p>"
            + "<h3>What renders</h3>"
            + "<p>Block &amp; inline flow, the box model (margins, borders, padding), text wrapping with "
            + "real font metrics, colors, backgrounds, lists, links, and images. Modern SPAs, video, and "
            + "WebGL are out of scope by construction — see <code>DESIGN.md</code>.</p>"
            + "<script>document.getElementById('js').textContent = "
            + "'JavaScript status: enabled (Nashorn).';</script>"
            + "</body></html>";
        byte[] bytes = html.getBytes(Charset.forName("UTF-8"));
        return "data:text/html;base64," + Base64.getEncoder().encodeToString(bytes);
    }
}
