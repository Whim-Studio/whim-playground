package com.whim.tacticalnexus.ui;

import com.whim.tacticalnexus.domain.Player;
import com.whim.tacticalnexus.state.StateManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * EAST component: a strictly formatted dashboard of the player's stats.
 *
 * <p>Shows HP, ATK, DEF, the three key colors, Gold, EXP and Level. It is a pure
 * view — {@link #refresh()} re-reads {@code stateManager.current()} and repaints
 * the labels.
 */
public final class StatusPanel extends JPanel {

    private static final Color BACKGROUND = new Color(30, 30, 36);
    private static final Color LABEL_FG = new Color(170, 170, 182);
    private static final Color VALUE_FG = new Color(236, 236, 242);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font VALUE_FONT = new Font("Monospaced", Font.BOLD, 15);

    private final StateManager stateManager;

    private final JLabel hp = value();
    private final JLabel atk = value();
    private final JLabel def = value();
    private final JLabel yellow = value();
    private final JLabel blue = value();
    private final JLabel red = value();
    private final JLabel gold = value();
    private final JLabel exp = value();
    private final JLabel level = value();
    private final JLabel floor = value();
    private final JLabel message = new JLabel(" ");

    public StatusPanel(StateManager stateManager) {
        this.stateManager = stateManager;
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(190, 0));

        JLabel title = new JLabel("TACTICAL NEXUS");
        title.setForeground(new Color(38, 198, 188));
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(title);
        add(Box.createVerticalStrut(12));

        JPanel grid = new JPanel(new GridLayout(0, 2, 6, 6));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        addRow(grid, "HP", hp);
        addRow(grid, "ATK", atk);
        addRow(grid, "DEF", def);
        addRow(grid, "Yellow", yellow);
        addRow(grid, "Blue", blue);
        addRow(grid, "Red", red);
        addRow(grid, "Gold", gold);
        addRow(grid, "EXP", exp);
        addRow(grid, "Level", level);
        addRow(grid, "Floor", floor);
        add(grid);

        add(Box.createVerticalStrut(14));
        message.setForeground(LABEL_FG);
        message.setFont(LABEL_FONT);
        message.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(message);

        add(Box.createVerticalGlue());

        JLabel help = new JLabel("<html>Arrows: move<br>Ctrl+Z: undo<br>Ctrl+Y: redo</html>");
        help.setForeground(new Color(120, 120, 132));
        help.setFont(new Font("SansSerif", Font.PLAIN, 11));
        help.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(help);

        refresh();
    }

    /** Re-read the current state and repaint every value label. */
    public void refresh() {
        Player p = stateManager.current().player();
        hp.setText(Integer.toString(p.hp()));
        atk.setText(Integer.toString(p.atk()));
        def.setText(Integer.toString(p.def()));
        yellow.setText(Integer.toString(p.yellowKeys()));
        blue.setText(Integer.toString(p.blueKeys()));
        red.setText(Integer.toString(p.redKeys()));
        gold.setText(Integer.toString(p.gold()));
        exp.setText(Integer.toString(p.exp()));
        level.setText(Integer.toString(p.level()));
        floor.setText(Integer.toString(stateManager.current().floorIndex()));
    }

    /** Display the controller's most recent action message. */
    public void setMessage(String text) {
        message.setText(text == null || text.isEmpty() ? " " : text);
    }

    private void addRow(JPanel grid, String labelText, JLabel valueLabel) {
        JLabel label = new JLabel(labelText);
        label.setForeground(LABEL_FG);
        label.setFont(LABEL_FONT);
        grid.add(label);
        grid.add(valueLabel);
    }

    private static JLabel value() {
        JLabel l = new JLabel("0");
        l.setForeground(VALUE_FG);
        l.setFont(VALUE_FONT);
        return l;
    }
}
