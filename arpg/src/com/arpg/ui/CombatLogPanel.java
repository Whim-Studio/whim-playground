package com.arpg.ui;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Scrolling, color-coded combat log backed by a {@link JTextPane}.
 * Damage is red, healing green, buffs blue, debuffs purple, loot gold and
 * plain narration muted gray. All mutation happens on the EDT.
 */
public class CombatLogPanel extends JPanel {

    private final JTextPane textPane;
    private final StyledDocument doc;

    public CombatLogPanel() {
        super(new BorderLayout());
        setBackground(UiTheme.BG_PANEL);
        setBorder(new EmptyBorder(6, 6, 6, 6));

        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setBackground(UiTheme.BG_DARK);
        textPane.setFont(UiTheme.MONO);
        doc = textPane.getStyledDocument();

        JScrollPane scroll = new JScrollPane(textPane,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);
    }

    /** Append a neutral narration line. */
    public void log(String message) {
        append(message, UiTheme.FG_MUTED, false);
    }

    public void logDamage(String message) {
        append(message, UiTheme.DAMAGE, true);
    }

    public void logHeal(String message) {
        append(message, UiTheme.HEAL, true);
    }

    public void logBuff(String message) {
        append(message, UiTheme.BUFF, false);
    }

    public void logDebuff(String message) {
        append(message, UiTheme.DEBUFF, false);
    }

    public void logLoot(String message) {
        append(message, UiTheme.LOOT, true);
    }

    public void logDeath(String message) {
        append(message, UiTheme.DAMAGE, true);
    }

    /** Replace the whole log with the given lines (used on snapshot refresh). */
    public void setLines(java.util.List<String> lines) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setLines(lines);
                }
            });
            return;
        }
        textPane.setText("");
        if (lines != null) {
            for (int i = 0; i < lines.size(); i++) {
                append(lines.get(i), UiTheme.FG_MUTED, false);
            }
        }
    }

    private void append(final String message, final Color color, final boolean bold) {
        if (message == null) {
            return;
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    append(message, color, bold);
                }
            });
            return;
        }
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, color);
        StyleConstants.setBold(attrs, bold);
        StyleConstants.setFontFamily(attrs, "Monospaced");
        StyleConstants.setFontSize(attrs, 12);
        try {
            doc.insertString(doc.getLength(), message + "\n", attrs);
        } catch (BadLocationException ignored) {
            // Position is always valid (end of document); nothing to recover.
        }
        textPane.setCaretPosition(doc.getLength());
    }
}
