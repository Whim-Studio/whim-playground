package com.whim.civ.ui;

import com.whim.civ.domain.Civilization;
import com.whim.civ.domain.GameState;
import com.whim.civ.domain.TechType;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;

/**
 * The cascading tech tree, rendered as status-coloured cards. Known techs are green, the
 * tech currently being researched is highlighted, techs whose prerequisites are all known
 * are clickable (selecting one calls {@link Civilization#setResearching}), and locked techs
 * are greyed with their missing prerequisites shown. A tech is <i>researchable</i> iff it is
 * not yet known and every prerequisite is known.
 */
public final class TechTreePanel extends JPanel {

    private final GameState state;
    private Civilization civ;
    private final JLabel header = new JLabel("Research");
    private final JPanel grid = new JPanel(new GridLayout(0, 2, 6, 6));
    private Runnable onChanged = new Runnable() {
        public void run() { }
    };

    public TechTreePanel(GameState state) {
        this.state = state;
        setLayout(new BorderLayout(0, 6));
        setBackground(UiTheme.PANEL_BG);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setPreferredSize(new Dimension(280, 400));

        header.setFont(UiTheme.H1);
        header.setForeground(UiTheme.PANEL_FG);
        add(header, BorderLayout.NORTH);

        grid.setOpaque(false);
        JScrollPane sp = new JScrollPane(grid,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.getViewport().setOpaque(false);
        sp.setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        add(sp, BorderLayout.CENTER);
    }

    public void setOnChanged(Runnable r) {
        this.onChanged = r != null ? r : this.onChanged;
    }

    public void setCivilization(Civilization civ) {
        this.civ = civ;
        rebuild();
    }

    /** Refresh after research progress / a tech unlock without changing the civ. */
    public void refresh() {
        rebuild();
    }

    private boolean isResearchable(TechType t) {
        if (civ.knows(t)) {
            return false;
        }
        List<TechType> prereqs = t.getPrereqs();
        for (int i = 0; i < prereqs.size(); i++) {
            if (!civ.knows(prereqs.get(i))) {
                return false;
            }
        }
        return true;
    }

    private void rebuild() {
        grid.removeAll();
        if (civ == null) {
            header.setText("Research");
            grid.revalidate();
            grid.repaint();
            return;
        }

        TechType current = civ.getResearching();
        if (current != null) {
            header.setText("<html>Researching: <b>" + pretty(current) + "</b><br>"
                    + civ.getResearchBeakers() + " / " + current.getBaseCost() + " beakers</html>");
        } else {
            header.setText("<html>No active research<br>Pick a tech below.</html>");
        }

        for (TechType t : TechType.values()) {
            grid.add(card(t, current));
        }
        grid.revalidate();
        grid.repaint();
    }

    private JButton card(final TechType t, TechType current) {
        boolean known = civ.knows(t);
        boolean researchable = isResearchable(t);
        boolean isCurrent = t == current;

        JButton b = new JButton();
        b.setFocusable(false);
        b.setHorizontalAlignment(JButton.LEFT);
        b.setFont(UiTheme.BODY);
        b.setText(label(t, known));

        Color bg;
        Color fg = Color.WHITE;
        if (known) {
            bg = new Color(46, 110, 64);
        } else if (isCurrent) {
            bg = new Color(40, 90, 150);
        } else if (researchable) {
            bg = new Color(70, 80, 96);
        } else {
            bg = new Color(48, 50, 58);
            fg = new Color(150, 150, 156);
        }
        b.setBackground(bg);
        b.setForeground(fg);
        b.setBorder(BorderFactory.createLineBorder(
                isCurrent ? UiTheme.SELECTION : new Color(0, 0, 0, 80),
                isCurrent ? 2 : 1));
        b.setEnabled(researchable && !isCurrent);
        b.setToolTipText(tooltip(t, known));

        if (researchable) {
            b.addActionListener(e -> {
                civ.setResearching(t);
                rebuild();
                onChanged.run();
            });
        }
        return b;
    }

    private String label(TechType t, boolean known) {
        String tag = known ? "✓ " : "";
        return "<html><b>" + tag + pretty(t) + "</b><br><span style='font-size:9px'>"
                + t.getBaseCost() + "b</span></html>";
    }

    private String tooltip(TechType t, boolean known) {
        StringBuilder sb = new StringBuilder("<html>");
        sb.append("<b>").append(pretty(t)).append("</b><br>");
        sb.append("Cost: ").append(t.getBaseCost()).append(" beakers<br>");
        List<TechType> p = t.getPrereqs();
        if (p.isEmpty()) {
            sb.append("No prerequisites");
        } else {
            sb.append("Requires:");
            for (int i = 0; i < p.size(); i++) {
                TechType pre = p.get(i);
                sb.append("<br>&nbsp;&nbsp;")
                        .append(civ.knows(pre) ? "✓ " : "✗ ")
                        .append(pretty(pre));
            }
        }
        if (known) {
            sb.append("<br><i>Already known</i>");
        }
        sb.append("</html>");
        return sb.toString();
    }

    private static String pretty(TechType t) {
        String[] parts = t.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return sb.toString();
    }
}
