package com.whimkit.ui;

import com.whimkit.dom.Document;
import com.whimkit.dom.Element;
import com.whimkit.dom.Node;
import com.whimkit.dom.TextNode;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * The top-level browser window: a shared toolbar (back/forward/reload/address/go,
 * new-tab, dev-tools), a {@link JTabbedPane} of {@link BrowserTab}s, a status bar,
 * and a toggleable developer panel (DOM outline + JavaScript console).
 *
 * <p>All construction and mutation happen on the EDT (see {@link com.whimkit.app.Main}).</p>
 */
public final class BrowserFrame extends JFrame {

    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final List<BrowserTab> tabs = new ArrayList<BrowserTab>();
    private final JTextField address = new JTextField();
    private final JButton backBtn = new JButton("←");
    private final JButton fwdBtn = new JButton("→");
    private final JButton reloadBtn = new JButton("↻");
    private final JLabel statusBar = new JLabel(" ");
    private final JTextArea domOutline = new JTextArea();
    private final JTextField jsConsole = new JTextField();
    private final JTextArea jsOutput = new JTextArea();
    private JSplitPane devSplit;
    private boolean devVisible = false;

    private final JComponent content;

    public BrowserFrame() {
        super("WhimKit Browser");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1180, 820);
        setLocationRelativeTo(null);

        add(buildToolbar(), BorderLayout.NORTH);
        content = buildCenter();
        add(content, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        tabbedPane.addChangeListener(e -> {
            BrowserTab t = selectedTab();
            if (t != null) { updateChrome(t); refreshDevTools(t); }
        });
    }

    private JToolBar buildToolbar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        backBtn.setToolTipText("Back");
        fwdBtn.setToolTipText("Forward");
        reloadBtn.setToolTipText("Reload");
        backBtn.addActionListener(e -> { BrowserTab t = selectedTab(); if (t != null) t.back(); });
        fwdBtn.addActionListener(e -> { BrowserTab t = selectedTab(); if (t != null) t.forward(); });
        reloadBtn.addActionListener(e -> { BrowserTab t = selectedTab(); if (t != null) t.reload(); });

        address.addActionListener(e -> {
            BrowserTab t = selectedTab();
            if (t != null) t.navigate(address.getText(), true);
        });
        JButton go = new JButton("Go");
        go.addActionListener(e -> { BrowserTab t = selectedTab(); if (t != null) t.navigate(address.getText(), true); });

        JButton newTab = new JButton("+");
        newTab.setToolTipText("New tab");
        newTab.addActionListener(e -> addTab("about:whimkit"));

        JButton dev = new JButton("DevTools");
        dev.addActionListener(e -> toggleDevTools());

        bar.add(backBtn); bar.add(fwdBtn); bar.add(reloadBtn);
        bar.add(Box.createHorizontalStrut(6));
        bar.add(address);
        bar.add(go);
        bar.add(Box.createHorizontalStrut(6));
        bar.add(newTab);
        bar.add(dev);
        return bar;
    }

    private JComponent buildCenter() {
        // Dev panel (DOM outline + JS console), hidden until toggled.
        domOutline.setEditable(false);
        domOutline.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        jsOutput.setEditable(false);
        jsOutput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        jsConsole.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        jsConsole.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { runConsole(); }
        });

        JPanel consolePanel = new JPanel(new BorderLayout());
        consolePanel.add(new JScrollPane(jsOutput), BorderLayout.CENTER);
        JPanel inputRow = new JPanel(new BorderLayout());
        inputRow.add(new JLabel(" JS > "), BorderLayout.WEST);
        inputRow.add(jsConsole, BorderLayout.CENTER);
        consolePanel.add(inputRow, BorderLayout.SOUTH);
        consolePanel.setBorder(BorderFactory.createTitledBorder("Console"));

        JScrollPane outlinePane = new JScrollPane(domOutline);
        outlinePane.setBorder(BorderFactory.createTitledBorder("DOM"));

        JSplitPane devPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, outlinePane, consolePanel);
        devPanel.setResizeWeight(0.5);

        devSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPane, devPanel);
        devSplit.setResizeWeight(1.0);
        devSplit.setDividerSize(0);
        devPanel.setVisible(false);
        return devSplit;
    }

    private JComponent buildStatusBar() {
        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        return statusBar;
    }

    // --- tab management ---------------------------------------------------

    public BrowserTab addTab(String url) {
        final BrowserTab tab = new BrowserTab(this);
        tabs.add(tab);
        tabbedPane.addTab("New Tab", tab.getComponent());
        int idx = tabbedPane.getTabCount() - 1;
        tabbedPane.setSelectedIndex(idx);
        SwingUtilities.invokeLater(() -> tab.navigate(url, true));
        return tab;
    }

    private BrowserTab selectedTab() {
        int i = tabbedPane.getSelectedIndex();
        return (i >= 0 && i < tabs.size()) ? tabs.get(i) : null;
    }

    /** Called by a tab when its chrome-relevant state changes. */
    public void updateChrome(BrowserTab tab) {
        if (tab != selectedTab()) return;
        int i = tabs.indexOf(tab);
        if (i >= 0) {
            String t = tab.getTitle();
            tabbedPane.setTitleAt(i, t.length() > 24 ? t.substring(0, 24) + "…" : t);
        }
        if (!address.isFocusOwner()) address.setText(tab.getCurrentUrl());
        setTitle(tab.getTitle() + " — WhimKit Browser");
        backBtn.setEnabled(tab.canBack());
        fwdBtn.setEnabled(tab.canForward());
        statusBar.setText(tab.getStatus().isEmpty() ? " " : tab.getStatus());
    }

    // --- dev tools --------------------------------------------------------

    private void toggleDevTools() {
        devVisible = !devVisible;
        JComponent devPanel = (JComponent) devSplit.getBottomComponent();
        devPanel.setVisible(devVisible);
        devSplit.setDividerSize(devVisible ? 6 : 0);
        if (devVisible) devSplit.setDividerLocation(0.68);
        BrowserTab t = selectedTab();
        if (t != null) refreshDevTools(t);
        revalidate();
    }

    public void refreshDevTools(BrowserTab tab) {
        if (!devVisible || tab != selectedTab()) return;
        Document doc = (tab.getPage() != null) ? tab.getPage().document : null;
        StringBuilder sb = new StringBuilder();
        if (doc != null && doc.getDocumentElement() != null) {
            outline(doc.getDocumentElement(), 0, sb);
        } else {
            sb.append("(no document)");
        }
        domOutline.setText(sb.toString());
        domOutline.setCaretPosition(0);
    }

    private void outline(Node node, int depth, StringBuilder sb) {
        if (depth > 40) return;
        for (int i = 0; i < depth; i++) sb.append("  ");
        if (node instanceof Element) {
            Element e = (Element) node;
            sb.append('<').append(e.getTagName());
            if (e.getId() != null) sb.append(" #").append(e.getId());
            if (!e.getClassName().isEmpty()) sb.append(" .").append(e.getClassName());
            sb.append(">\n");
            for (Node c : e.getChildNodes()) outline(c, depth + 1, sb);
        } else if (node instanceof TextNode) {
            String t = ((TextNode) node).getData().trim();
            if (!t.isEmpty()) sb.append('"').append(t.length() > 50 ? t.substring(0, 50) + "…" : t).append("\"\n");
            else sb.setLength(sb.length() - depth * 2); // drop empty-text indent
        }
    }

    private void runConsole() {
        String src = jsConsole.getText();
        if (src.trim().isEmpty()) return;
        jsOutput.append("> " + src + "\n");
        jsConsole.setText("");
        BrowserTab t = selectedTab();
        if (t == null || t.getJs() == null) { jsOutput.append("(no JS runtime)\n"); return; }
        if (!t.getJs().isAvailable()) { jsOutput.append("(Nashorn unavailable on this JDK)\n"); return; }
        t.getJs().execute(src, "console");
    }
}
