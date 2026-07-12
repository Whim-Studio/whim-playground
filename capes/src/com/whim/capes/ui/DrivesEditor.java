package com.whim.capes.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.whim.capes.model.Drive;
import com.whim.capes.model.DriveType;

/**
 * Editor for a detailed super-powered character's five Drives (p.74): five
 * DriveType pickers with Strength spinners (1-5) and a live total that must
 * equal nine. An "Undifferentiated" toggle (p.74) hides the grid for bit-part
 * characters, who keep a single Debt stack instead.
 */
public final class DrivesEditor extends JPanel {
    private final JCheckBox undiff = new JCheckBox("Undifferentiated (single Debt stack, no detailed Drives)");
    private final JPanel grid = new JPanel();
    private final JLabel total = new JLabel();
    private final List<DriveRow> driveRows = new ArrayList<DriveRow>();

    public DrivesEditor() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder("Drives (Strengths total 9)"));
        setBackground(Palette.PANEL);

        undiff.setOpaque(false);
        undiff.setAlignmentX(LEFT_ALIGNMENT);
        undiff.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { syncEnabled(); updateTotal(); }
        });
        add(undiff);

        grid.setLayout(new BoxLayout(grid, BoxLayout.Y_AXIS));
        grid.setOpaque(false);
        grid.setAlignmentX(LEFT_ALIGNMENT);
        // five default Drives summing to nine (a classic 3/2/2/1/1 spread)
        DriveType[] defs = { DriveType.JUSTICE, DriveType.LOVE, DriveType.HOPE, DriveType.DUTY, DriveType.TRUTH };
        int[] str = { 3, 2, 2, 1, 1 };
        for (int i = 0; i < 5; i++) {
            DriveRow r = new DriveRow(defs[i], str[i]);
            driveRows.add(r);
            grid.add(r);
        }
        add(grid);

        total.setAlignmentX(LEFT_ALIGNMENT);
        add(total);
        updateTotal();
    }

    public boolean isUndifferentiated() { return undiff.isSelected(); }

    /** Collected Drives (empty if undifferentiated). */
    public List<Drive> getDrives() {
        List<Drive> out = new ArrayList<Drive>();
        if (undiff.isSelected()) return out;
        for (DriveRow r : driveRows) {
            out.add(new Drive((DriveType) r.type.getSelectedItem(), (Integer) r.strength.getValue()));
        }
        return out;
    }

    public int strengthTotal() {
        int t = 0;
        for (DriveRow r : driveRows) t += (Integer) r.strength.getValue();
        return t;
    }

    private void syncEnabled() {
        boolean on = !undiff.isSelected();
        for (DriveRow r : driveRows) { r.type.setEnabled(on); r.strength.setEnabled(on); }
    }

    private void updateTotal() {
        if (undiff.isSelected()) {
            total.setText("  Undifferentiated: Overdrawn above 5, Stake up to 3 per Conflict.");
            total.setForeground(Palette.MUTED);
            return;
        }
        int t = strengthTotal();
        total.setText("  Total Strength: " + t + " / 9");
        total.setForeground(t == 9 ? Palette.CONTROL : Palette.VILLAIN_RED);
    }

    private final class DriveRow extends JPanel {
        final JComboBox<DriveType> type = new JComboBox<DriveType>(DriveType.values());
        final JSpinner strength = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));

        DriveRow(DriveType t, int s) {
            setLayout(new FlowLayout(FlowLayout.LEFT, 4, 1));
            setOpaque(false);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            type.setSelectedItem(t);
            strength.setValue(s);
            strength.setPreferredSize(new Dimension(52, 26));
            ChangeListener cl = new ChangeListener() {
                @Override public void stateChanged(ChangeEvent e) { updateTotal(); }
            };
            strength.addChangeListener(cl);
            add(new JLabel("Strength"));
            add(strength);
            add(type);
        }
    }

    static final Color HINT = Palette.MUTED;
}
