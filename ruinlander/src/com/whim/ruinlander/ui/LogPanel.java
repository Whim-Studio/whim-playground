package com.whim.ruinlander.ui;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

/** Scrolling action log along the bottom of the frame. */
public class LogPanel extends JScrollPane {

    private final JTextArea area = new JTextArea();

    public LogPanel() {
        super(new JTextArea());
        // Replace the placeholder viewport view with our configured area.
        setViewportView(area);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(new Color(14, 13, 12));
        area.setForeground(new Color(170, 200, 150));
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        setPreferredSize(new Dimension(880, 120));
    }

    /** Append a line and scroll to the newest entry. Call on the EDT. */
    public void append(String line) {
        area.append(line + "\n");
        area.setCaretPosition(area.getDocument().getLength());
    }
}
