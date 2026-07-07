package com.whim.b5wars.ui;

import com.whim.b5wars.engine.GameEvent;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/** Scrolling combat/rules log fed by the {@link GameEvent} lists engine calls return. */
public final class LogPanel extends JPanel implements GameListener {

    private final JTextPane pane = new JTextPane();

    public LogPanel() {
        setLayout(new BorderLayout());
        setBackground(UiTheme.PANEL_BG);
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JLabel title = new JLabel("Combat Log");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.FONT_HEADER);
        add(title, BorderLayout.NORTH);

        pane.setEditable(false);
        pane.setBackground(new Color(12, 14, 20));
        pane.setFont(UiTheme.FONT_MONO);
        JScrollPane scroll = new JScrollPane(pane);
        scroll.setPreferredSize(new Dimension(360, 200));
        add(scroll, BorderLayout.CENTER);
    }

    @Override
    public void gameChanged() {
        // Log only reacts to explicit events.
    }

    @Override
    public void logEvents(List<GameEvent> events) {
        if (events == null) {
            return;
        }
        for (GameEvent e : events) {
            append(e);
        }
    }

    private void append(GameEvent e) {
        StyledDocument doc = pane.getStyledDocument();
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, colorFor(e.getType()));
        try {
            doc.insertString(doc.getLength(), "[" + e.getType() + "] " + e.getMessage() + "\n", a);
        } catch (BadLocationException ignored) {
            // append is always at the end; ignore
        }
        pane.setCaretPosition(doc.getLength());
    }

    private static Color colorFor(String type) {
        if (type == null) {
            return UiTheme.TEXT;
        }
        switch (type) {
            case "FIRE": return new Color(120, 200, 255);
            case "HIT": return new Color(255, 180, 90);
            case "CRIT": return new Color(255, 120, 120);
            case "MISS": return new Color(150, 160, 180);
            case "MOVE": return new Color(130, 220, 160);
            case "VICTORY": return new Color(255, 232, 120);
            case "PHASE": return new Color(190, 170, 255);
            default: return UiTheme.TEXT;
        }
    }
}
