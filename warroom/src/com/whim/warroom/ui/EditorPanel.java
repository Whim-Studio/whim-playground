package com.whim.warroom.ui;

import com.whim.warroom.domain.Biome;
import com.whim.warroom.domain.Era;
import com.whim.warroom.domain.Faction;
import com.whim.warroom.domain.Stance;
import com.whim.warroom.domain.UnitCatalog;
import com.whim.warroom.domain.UnitType;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

/**
 * WEST tool palette for Simulation (editor) mode: terrain (dominant biome +
 * Regenerate, biome paint brush), the unit catalog grouped by {@link Era},
 * faction + stance selectors, and the mouse-tool picker (place / route /
 * select-box / marker). All controls write through the {@link SandboxController}.
 */
public final class EditorPanel extends JPanel {

    private final SandboxController ctl;
    private final ButtonGroup toolGroup = new ButtonGroup();
    private final ButtonGroup unitGroup = new ButtonGroup();

    public EditorPanel(SandboxController ctl) {
        this.ctl = ctl;
        setBackground(ThemeUI.BG_PANEL);
        setPreferredSize(new Dimension(232, 100));
        setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        col.add(header("WAR ROOM"));
        col.add(sub("Simulation / editor"));
        col.add(gap(8));

        col.add(sectionLabel("TERRAIN"));
        col.add(buildBiomePicker());
        col.add(gap(4));
        col.add(buildTerrainTools());
        col.add(gap(10));

        col.add(sectionLabel("FACTION / STANCE"));
        col.add(buildFactionStance());
        col.add(gap(10));

        col.add(sectionLabel("TOOLS"));
        col.add(buildToolButtons());
        col.add(gap(10));

        col.add(sectionLabel("UNIT CATALOG"));
        col.add(buildUnitCatalog());
        col.add(gap(6));
        col.add(hint("Click field to place. ROUTE tool: click a unit, click waypoints, right-click to finish."));

        JScrollPane sp = new JScrollPane(col,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        setLayout(new java.awt.BorderLayout());
        add(sp, java.awt.BorderLayout.CENTER);
    }

    // ---------- terrain ----------
    private JPanel buildBiomePicker() {
        JPanel p = row();
        p.add(small("Dominant"));
        final JComboBox<Biome> combo = new JComboBox<Biome>(Biome.values());
        combo.setSelectedItem(ctl.getDominantBiome());
        combo.setMaximumSize(new Dimension(120, 26));
        combo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    ctl.setDominantBiome((Biome) combo.getSelectedItem());
            }
        });
        p.add(combo);
        return p;
    }

    private JPanel buildTerrainTools() {
        JPanel p = row();
        JButton regen = themed(new JButton("Regenerate"));
        regen.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ctl.regenerateMap();
                ctl.getField().repaint();
            }
        });
        p.add(regen);

        final JComboBox<Biome> brush = new JComboBox<Biome>(Biome.values());
        brush.setSelectedItem(ctl.getBrushBiome());
        brush.setMaximumSize(new Dimension(96, 26));
        brush.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    ctl.setBrushBiome((Biome) brush.getSelectedItem());
            }
        });
        p.add(brush);
        return p;
    }

    // ---------- faction / stance ----------
    private JPanel buildFactionStance() {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 4));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(220, 60));

        final JComboBox<Faction> fac = new JComboBox<Faction>(new Faction[]{Faction.BLUE, Faction.RED, Faction.NEUTRAL});
        fac.setSelectedItem(ctl.getBrushFaction());
        fac.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    ctl.setBrushFaction((Faction) fac.getSelectedItem());
            }
        });
        final JComboBox<Stance> st = new JComboBox<Stance>(Stance.values());
        st.setSelectedItem(ctl.getBrushStance());
        st.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    ctl.setBrushStance((Stance) st.getSelectedItem());
            }
        });
        p.add(fac);
        p.add(st);
        return p;
    }

    // ---------- tools ----------
    private JPanel buildToolButtons() {
        JPanel p = new JPanel(new GridLayout(0, 2, 4, 4));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(220, 120));
        p.add(toolButton("Place Unit", SandboxController.Tool.PLACE_UNIT, true));
        p.add(toolButton("Draw Route", SandboxController.Tool.ROUTE, false));
        p.add(toolButton("Select Box", SandboxController.Tool.SELECT, false));
        p.add(toolButton("Marker", SandboxController.Tool.MARKER, false));
        p.add(toolButton("Paint Land", SandboxController.Tool.PAINT_TERRAIN, false));
        return p;
    }

    private JToggleButton toolButton(String label, final SandboxController.Tool tool, boolean selected) {
        final JToggleButton b = new JToggleButton(label, selected);
        style(b);
        toolGroup.add(b);
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { ctl.setTool(tool); }
        });
        return b;
    }

    // ---------- unit catalog ----------
    private JPanel buildUnitCatalog() {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        boolean first = true;
        Era[] eras = Era.values();
        for (int i = 0; i < eras.length; i++) {
            Era era = eras[i];
            List<UnitType> types = UnitCatalog.byEra(era);
            if (types.isEmpty()) continue;
            wrap.add(small(era.label().toUpperCase()));
            for (int j = 0; j < types.size(); j++) {
                UnitType t = types.get(j);
                wrap.add(unitButton(t, first));
                first = false;
            }
            wrap.add(gap(4));
        }
        return wrap;
    }

    private JToggleButton unitButton(final UnitType t, boolean selected) {
        String txt = t.getName() + "   H" + (int) t.getMaxHealth()
                + " A" + (int) t.getAttack() + " R" + (int) t.getRange();
        final JToggleButton b = new JToggleButton(txt, selected);
        style(b);
        b.setHorizontalAlignment(JButton.LEFT);
        b.setMaximumSize(new Dimension(220, 26));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        unitGroup.add(b);
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ctl.setBrushType(t);
                ctl.setTool(SandboxController.Tool.PLACE_UNIT);
                // reflect tool change in the tool button group
                selectToolButton("Place Unit");
            }
        });
        if (selected) ctl.setBrushType(t);
        return b;
    }

    private void selectToolButton(String label) {
        java.util.Enumeration<AbstractButton> en = toolGroup.getElements();
        while (en.hasMoreElements()) {
            AbstractButton b = en.nextElement();
            if (label.equals(b.getText())) { b.setSelected(true); return; }
        }
    }

    // ---------- small widget helpers ----------
    private JPanel row() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(220, 30));
        return p;
    }

    private JButton themed(JButton b) { style(b); return b; }

    private void style(AbstractButton b) {
        b.setFont(ThemeUI.UI);
        b.setForeground(ThemeUI.TEXT);
        b.setBackground(ThemeUI.BG_PANEL_2);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(4, 6, 4, 6));
        b.setContentAreaFilled(true);
    }

    private JLabel header(String s) {
        JLabel l = new JLabel(s);
        l.setFont(ThemeUI.TITLE);
        l.setForeground(ThemeUI.ACCENT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }
    private JLabel sub(String s) {
        JLabel l = new JLabel(s);
        l.setFont(ThemeUI.UI_SMALL);
        l.setForeground(ThemeUI.TEXT_MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }
    private JLabel sectionLabel(String s) {
        JLabel l = new JLabel(s);
        l.setFont(ThemeUI.UI_BOLD);
        l.setForeground(ThemeUI.TEXT_DIM);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(2, 0, 2, 0));
        return l;
    }
    private JLabel small(String s) {
        JLabel l = new JLabel(s);
        l.setFont(ThemeUI.UI_SMALL);
        l.setForeground(ThemeUI.TEXT_DIM);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }
    private JLabel hint(final String s) {
        JLabel l = new JLabel("<html><div style='width:200px'>" + s + "</div></html>");
        l.setFont(ThemeUI.UI_SMALL);
        l.setForeground(ThemeUI.TEXT_MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }
    private Component gap(int h) { return Box.createRigidArea(new Dimension(1, h)); }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(ThemeUI.BORDER);
        g.fillRect(getWidth() - 1, 0, 1, getHeight());
    }
}
