package com.whim.capes.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;

import com.whim.capes.model.EventLog;
import com.whim.capes.model.EventLogEntry;

/**
 * The shared event-log side panel (required by the spec for debugging and for
 * players to review play). Subscribes to an {@link EventLog} and appends each
 * entry live on the EDT.
 */
public final class EventLogPanel extends JPanel implements EventLog.Listener {
    private final DefaultListModel<String> model = new DefaultListModel<String>();
    private final JList<String> list = new JList<String>(model);

    public EventLogPanel(EventLog log) {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(320, 100));
        setBackground(Palette.PAPER);
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Palette.PANEL_EDGE));

        JLabel header = new JLabel("  Event Log");
        header.setFont(Palette.HEADING);
        header.setOpaque(true);
        header.setBackground(Palette.INK);
        header.setForeground(Palette.PAPER);
        header.setPreferredSize(new Dimension(320, 28));
        add(header, BorderLayout.NORTH);

        list.setFont(Palette.MONO);
        add(new JScrollPane(list), BorderLayout.CENTER);

        log.addListener(this);
        for (EventLogEntry e : log.entries()) append(e);
    }

    @Override public void onEntry(final EventLogEntry entry) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { append(entry); }
        });
    }

    private void append(EventLogEntry e) {
        model.addElement(String.format("%03d %s", e.sequence(), e.toString()));
        int last = model.getSize() - 1;
        if (last >= 0) list.ensureIndexIsVisible(last);
    }
}
