package com.whim.necromunda.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.whim.necromunda.engine.GameState;
import com.whim.necromunda.engine.LogEntry;

/**
 * A scrolling action/dice log. Mirrors the {@link GameState} append-only log;
 * on refresh it re-renders all entries and scrolls to the bottom.
 */
public final class LogPanel extends JScrollPane {

    private final GameState state;
    private final JTextArea area = new JTextArea();
    private int rendered;

    public LogPanel(GameState state) {
        super(new JTextArea());
        this.state = state;
        // We created a throwaway in super(); swap in our configured area.
        setViewportView(area);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setBackground(new Color(0x16, 0x16, 0x1A));
        area.setForeground(new Color(0xD0, 0xD0, 0xD8));
        setBorder(BorderFactory.createTitledBorder("Action Log"));
        setPreferredSize(new Dimension(360, 260));
        refresh();
    }

    /** Re-render the log from game state and scroll to the latest line. */
    public void refresh() {
        StringBuilder sb = new StringBuilder();
        for (LogEntry e : state.log()) {
            sb.append(e.toString()).append('\n');
        }
        area.setText(sb.toString());
        area.setCaretPosition(area.getDocument().getLength());
        rendered = state.log().size();
    }

    public int renderedCount() {
        return rendered;
    }
}
