package com.whim.capes.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.whim.capes.model.Ability;
import com.whim.capes.model.AbilityKind;

/**
 * Editor for one ability column (Powers/Skills, Attitudes or Styles), shared by
 * the Freeform and Click-and-Lock flows. Each row is a name field, a rank
 * spinner and a "super" toggle (only meaningful for Styles), plus remove. An
 * Add button appends a blank row. Rank order can be set explicitly or left for
 * {@code CharacterFactory.renumberColumns} to assign 1-up by row order.
 */
public final class AbilityColumnEditor extends JPanel {
    private final AbilityKind kind;
    private final boolean defaultSuper;
    private final JPanel rows = new JPanel();
    private final List<Row> rowList = new ArrayList<Row>();

    public AbilityColumnEditor(String title, AbilityKind kind, boolean defaultSuper) {
        this.kind = kind;
        this.defaultSuper = defaultSuper;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder(title));
        setBackground(Palette.PANEL);

        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);
        JScrollPane sp = new JScrollPane(rows);
        sp.setPreferredSize(new Dimension(280, 220));
        sp.getVerticalScrollBar().setUnitIncrement(12);
        add(sp);

        JButton add = new JButton("+ Add " + kind.displayName());
        add.setAlignmentX(LEFT_ALIGNMENT);
        add.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { addRow("", 0, defaultSuper); }
        });
        add(add);
    }

    /** Clears all rows and repopulates from the given abilities (used by Click-and-Lock load). */
    public void setAbilities(List<Ability> abilities) {
        rowList.clear();
        rows.removeAll();
        for (Ability a : abilities) addRow(a.name(), a.score(), a.isSuperPowered());
        revalidate();
        repaint();
    }

    public void addRow(String name, int score, boolean superFlag) {
        Row r = new Row(name, score, superFlag);
        rowList.add(r);
        rows.add(r);
        revalidate();
        repaint();
    }

    /** Current abilities in row order (blank-named rows are skipped). */
    public List<Ability> getAbilities() {
        List<Ability> out = new ArrayList<Ability>();
        for (Row r : rowList) {
            String name = r.name.getText().trim();
            if (name.isEmpty()) continue;
            int score = (Integer) r.score.getValue();
            boolean sup = kind == AbilityKind.POWER || (kind == AbilityKind.STYLE && r.superBox.isSelected());
            out.add(new Ability(name, kind, score, sup));
        }
        return out;
    }

    public int count() { return getAbilities().size(); }

    /** The ability kind this column edits. */
    public AbilityKind kind() { return kind; }

    private final class Row extends JPanel {
        final JTextField name = new JTextField(14);
        final JSpinner score = new JSpinner(new SpinnerNumberModel(0, 0, 12, 1));
        final JCheckBox superBox = new JCheckBox("super");

        Row(String n, int s, boolean sup) {
            setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));
            setOpaque(false);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            name.setText(n);
            score.setValue(s);
            score.setPreferredSize(new Dimension(52, 26));
            superBox.setSelected(sup);
            superBox.setOpaque(false);
            add(new JLabel("#"));
            add(score);
            add(name);
            if (kind == AbilityKind.STYLE) add(superBox); // only Styles vary
            JButton del = new JButton("✕");
            del.setMargin(new java.awt.Insets(0, 6, 0, 6));
            del.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    rowList.remove(Row.this);
                    rows.remove(Row.this);
                    rows.revalidate();
                    rows.repaint();
                }
            });
            add(del);
        }
    }

    static Component vgap(int h) { return Box.createVerticalStrut(h); }
}
