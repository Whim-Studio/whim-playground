package com.whim.browser.ui;

import com.whim.browser.engine.BrowserEngine;
import com.whim.browser.engine.EngineCallback;
import com.whim.browser.model.HistoryManager;
import com.whim.browser.model.Tab;
import com.whim.browser.model.TabManager;
import com.whim.browser.model.WebResponse;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

/**
 * The main browser window. This is the entire Swing presentation layer for the
 * Java 8 desktop web browser: a top toolbar (Back / Forward / Refresh / address
 * bar / Go / loading indicator), a central {@link JEditorPane} that renders the
 * fetched HTML, and a bottom status bar.
 *
 * <p>The frame itself implements {@link EngineCallback}; it hands URLs to
 * {@link BrowserEngine#load(String, EngineCallback)} and receives the results
 * back on the Event Dispatch Thread. The engine performs all blocking network
 * work off the EDT and marshals {@code onStart}/{@code onResult} back via
 * {@code invokeLater}, so this class only ever touches Swing state on the EDT.</p>
 *
 * <p><b>Rendering limitations.</b> {@link JEditorPane}'s built-in
 * {@code HTMLEditorKit} understands only HTML 3.2 plus a small slice of CSS. It
 * has <i>no</i> JavaScript engine, <i>no</i> HTML5 semantics, and <i>no</i>
 * media/video support. Modern pages therefore render partially or not at all.
 * YouTube (and other HTML5-video pages) are detected upstream by the engine and
 * opened in the operating system's native browser instead; here we merely show
 * an informational page describing what happened.</p>
 */
public final class BrowserFrame extends JFrame implements EngineCallback {

    private static final long serialVersionUID = 1L;

    private final BrowserEngine engine = new BrowserEngine();
    private final TabManager tabs = new TabManager();

    private final JButton backButton = new JButton("← Back");
    private final JButton forwardButton = new JButton("Forward →");
    private final JButton refreshButton = new JButton("↻ Refresh");
    private final JButton goButton = new JButton("Go");
    private final JTextField addressField = new JTextField(48);
    private final JProgressBar loadingBar = new JProgressBar();

    private final JEditorPane contentPane = new JEditorPane();
    private final JLabel statusLabel = new JLabel("Ready.");

    /**
     * Distinguishes a fresh navigation (which should push a new history entry
     * once it succeeds) from a Back/Forward/Refresh navigation (which must
     * <i>not</i> re-push, otherwise the history would grow on every step).
     */
    private boolean historyNavigation = false;

    public BrowserFrame() {
        super("Whim Browser (Java 8 Swing)");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1024, 720);
        setLocationRelativeTo(null);

        // Start with a single tab, matching the "one tab at launch" rule.
        tabs.newTab();

        setLayout(new BorderLayout());
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildContentArea(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        wireActions();

        loadingBar.setVisible(false);
        updateButtons();
    }

    // ------------------------------------------------------------------ layout

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(6, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel navButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        navButtons.add(backButton);
        navButtons.add(forwardButton);
        navButtons.add(refreshButton);

        JPanel goPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        goPanel.add(goButton);
        loadingBar.setIndeterminate(true);
        loadingBar.setPreferredSize(new Dimension(90, 18));
        goPanel.add(loadingBar);

        bar.add(navButtons, BorderLayout.WEST);
        bar.add(addressField, BorderLayout.CENTER);
        bar.add(goPanel, BorderLayout.EAST);
        return bar;
    }

    private JScrollPane buildContentArea() {
        // A JEditorPane fixed to text/html renders with the built-in
        // HTMLEditorKit (HTML 3.2, no JS, no video — see class Javadoc).
        contentPane.setContentType("text/html");
        contentPane.setEditable(false);
        // We route every link click ourselves; never let the pane navigate.
        contentPane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    handleHyperlink(e);
                }
            }
        });
        return new JScrollPane(contentPane);
    }

    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        panel.add(statusLabel, BorderLayout.CENTER);
        return panel;
    }

    // ----------------------------------------------------------------- actions

    private void wireActions() {
        ActionListener goAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateTo(addressField.getText());
            }
        };
        goButton.addActionListener(goAction);
        addressField.addActionListener(goAction); // Enter key in the address bar.

        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goBack();
            }
        });
        forwardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                goForward();
            }
        });
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });
    }

    /**
     * Routes an activated hyperlink back through the engine. We prefer the
     * resolved {@link URL} (absolute), falling back to the raw description for
     * relative or non-standard hrefs.
     */
    private void handleHyperlink(HyperlinkEvent e) {
        URL url = e.getURL();
        String target = (url != null) ? url.toString() : e.getDescription();
        if (target != null && !target.trim().isEmpty()) {
            navigateTo(target);
        }
    }

    // -------------------------------------------------------------- navigation

    /**
     * Begins a fresh navigation to {@code url}. History is <i>not</i> pushed
     * here — it is pushed in {@link #onResult(WebResponse)} on the success path
     * only, so failed loads never pollute the back/forward stack.
     */
    public void navigateTo(String url) {
        if (url == null) {
            return;
        }
        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        historyNavigation = false;
        engine.load(trimmed, this);
    }

    private void goBack() {
        HistoryManager history = activeHistory();
        if (history.canGoBack()) {
            String url = history.back();
            reloadFromHistory(url);
        }
    }

    private void goForward() {
        HistoryManager history = activeHistory();
        if (history.canGoForward()) {
            String url = history.forward();
            reloadFromHistory(url);
        }
    }

    private void refresh() {
        String current = activeHistory().current();
        if (current != null && !current.isEmpty()) {
            // A refresh must not push a new history entry.
            reloadFromHistory(current);
        }
    }

    /**
     * Reloads a URL that already lives in the history stack (Back / Forward /
     * Refresh). The {@link #historyNavigation} flag suppresses the history push
     * that {@link #onResult(WebResponse)} would otherwise perform.
     */
    private void reloadFromHistory(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        historyNavigation = true;
        engine.load(url, this);
    }

    // ------------------------------------------------------- EngineCallback

    /**
     * {@inheritDoc}
     *
     * <p>Already invoked on the EDT by the engine, but we defensively marshal
     * so this method is safe to call from any thread.</p>
     */
    @Override
    public void onStart(final String url) {
        runOnEdt(new Runnable() {
            @Override
            public void run() {
                loadingBar.setVisible(true);
                setNavEnabled(false);
                statusLabel.setText("Loading … " + url);
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Renders the response into the content pane, updates the active tab's
     * title and (on success) history, then refreshes the address bar, status
     * bar and button enablement. Defensively marshalled onto the EDT.</p>
     */
    @Override
    public void onResult(final WebResponse response) {
        runOnEdt(new Runnable() {
            @Override
            public void run() {
                applyResult(response);
            }
        });
    }

    private void applyResult(WebResponse response) {
        loadingBar.setVisible(false);

        if (response == null) {
            statusLabel.setText("No response.");
            updateButtons();
            return;
        }

        Tab tab = tabs.active();
        tab.setLastResponse(response);

        if (response.isError()) {
            // Failure: render an error page but DO NOT touch history.
            renderHtml(errorHtml(response));
            statusLabel.setText("Error " + response.getStatusCode() + " — " + response.getUrl());
        } else {
            // Success (HTML or a YouTube redirect notice): render and record.
            if (response.isYouTube()) {
                renderHtml(youTubeHtml(response));
            } else {
                renderHtml(response.getBody());
            }

            String title = response.getTitle();
            tab.setTitle((title != null && !title.isEmpty()) ? title : response.getUrl());

            // Push history only for fresh navigations, never Back/Forward/Refresh.
            if (!historyNavigation) {
                tab.getHistory().visit(response.getUrl());
            }
            statusLabel.setText(response.getStatusCode() + " " + response.getUrl());
        }

        addressField.setText(response.getUrl());
        setTitle(tab.getTitle() + " — Whim Browser (Java 8 Swing)");
        updateButtons();
    }

    // -------------------------------------------------------------- rendering

    /**
     * Replaces the content pane's document with fresh HTML. We reset the pane's
     * document each time so stale state from the previous page cannot leak in,
     * and scroll back to the top like a real browser.
     */
    private void renderHtml(String html) {
        contentPane.setContentType("text/html");
        // A null/empty body still yields a valid (blank) document.
        contentPane.setText(html != null ? html : "");
        contentPane.setCaretPosition(0);
    }

    /** Builds the informational page shown when a YouTube page was redirected. */
    private String youTubeHtml(WebResponse response) {
        String title = response.getTitle() != null ? response.getTitle() : "YouTube video";
        String body = response.getBody() != null ? response.getBody() : "";
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><title>").append(escape(title)).append("</title></head>");
        sb.append("<body style=\"font-family:sans-serif; margin:24px;\">");
        sb.append("<h2>Opened in your native browser</h2>");
        sb.append("<p>This looks like a YouTube (HTML5 video) page. Swing's HTML 3.2 "
                + "renderer cannot play video, so it was opened in your operating "
                + "system's default browser instead.</p>");
        sb.append("<p><b>").append(escape(title)).append("</b></p>");
        sb.append(body);
        sb.append("<hr><p style=\"color:#888;\">").append(escape(response.getUrl())).append("</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    /** Builds a clean error page from the response body. */
    private String errorHtml(WebResponse response) {
        String body = response.getBody() != null ? response.getBody() : "The page could not be loaded.";
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><title>Error</title></head>");
        sb.append("<body style=\"font-family:sans-serif; margin:24px;\">");
        sb.append("<h2 style=\"color:#b00020;\">Unable to load page</h2>");
        sb.append("<p><b>HTTP status:</b> ").append(response.getStatusCode()).append("</p>");
        sb.append("<p><b>URL:</b> ").append(escape(response.getUrl())).append("</p>");
        sb.append("<div style=\"margin-top:12px;\">").append(body).append("</div>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ------------------------------------------------------------ button state

    private HistoryManager activeHistory() {
        return tabs.active().getHistory();
    }

    /** Enables/disables Back/Forward from the active tab's history. */
    private void updateButtons() {
        HistoryManager history = activeHistory();
        backButton.setEnabled(history.canGoBack());
        forwardButton.setEnabled(history.canGoForward());
        refreshButton.setEnabled(true);
        goButton.setEnabled(true);
        addressField.setEnabled(true);
    }

    /**
     * Disables navigation controls while a load is in flight. Back/Forward are
     * re-enabled by {@link #updateButtons()} once the result arrives.
     */
    private void setNavEnabled(boolean enabled) {
        backButton.setEnabled(enabled);
        forwardButton.setEnabled(enabled);
        refreshButton.setEnabled(enabled);
        goButton.setEnabled(enabled);
        addressField.setEnabled(enabled);
    }

    /** Runs {@code r} on the EDT now if already there, otherwise via invokeLater. */
    private static void runOnEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    /**
     * Loads an initial HTML document straight into the content pane without a
     * network round-trip. Used by {@code Main} to show the built-in welcome
     * page at startup.
     */
    public void showWelcomePage(String html) {
        renderHtml(html);
        statusLabel.setText("Ready.");
        addressField.setText("about:welcome");
    }
}
