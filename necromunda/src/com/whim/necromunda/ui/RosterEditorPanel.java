package com.whim.necromunda.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import com.whim.necromunda.engine.data.WeaponCatalogue;
import com.whim.necromunda.engine.roster.RosterRules;
import com.whim.necromunda.model.Armour;
import com.whim.necromunda.model.Fighter;
import com.whim.necromunda.model.FighterType;
import com.whim.necromunda.model.Gang;
import com.whim.necromunda.model.StatLine;
import com.whim.necromunda.model.Weapon;

/**
 * Gang creation / roster management UI (Milestone 3). Add/remove fighters, edit
 * name / role / armour, multi-select weapons from the catalogue, and see the
 * live gang rating plus roster-legality summary. Reads and mutates a {@link Gang}
 * model directly; the JSON save/load lives in {@link SaveManager}.
 */
public final class RosterEditorPanel extends JPanel {

    private final Gang gang;

    private final DefaultListModel<Fighter> rosterModel = new DefaultListModel<Fighter>();
    private final JList<Fighter> rosterList = new JList<Fighter>(rosterModel);

    private final JTextField nameField = new JTextField(14);
    private final JComboBox<FighterType> roleBox = new JComboBox<FighterType>(FighterType.values());
    private final JComboBox<Armour> armourBox = new JComboBox<Armour>(Armour.values());
    private final DefaultListModel<Weapon> weaponModel = new DefaultListModel<Weapon>();
    private final JList<Weapon> weaponList = new JList<Weapon>(weaponModel);

    private final JLabel summary = new JLabel(" ");

    private Fighter current;
    private boolean suppressEvents;

    public RosterEditorPanel(Gang gang) {
        this.gang = gang;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // --- left: roster list + add/remove ---
        JPanel left = new JPanel(new BorderLayout(4, 4));
        left.setBorder(BorderFactory.createTitledBorder("Roster — " + gang.name()));
        rosterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rosterList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelected();
            }
        });
        left.add(new JScrollPane(rosterList), BorderLayout.CENTER);
        JPanel addRemove = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("Add Fighter");
        JButton removeBtn = new JButton("Remove");
        addBtn.addActionListener(e -> addFighter());
        removeBtn.addActionListener(e -> removeFighter());
        addRemove.add(addBtn);
        addRemove.add(removeBtn);
        left.add(addRemove, BorderLayout.SOUTH);
        left.setPreferredSize(new Dimension(220, 380));
        add(left, BorderLayout.WEST);

        // --- center: fighter editor ---
        JPanel editor = new JPanel(new BorderLayout(6, 6));
        editor.setBorder(BorderFactory.createTitledBorder("Fighter"));

        JPanel form = new JPanel(new GridLayout(3, 2, 6, 6));
        form.add(new JLabel("Name:"));
        form.add(nameField);
        form.add(new JLabel("Role:"));
        form.add(roleBox);
        form.add(new JLabel("Armour:"));
        form.add(armourBox);
        editor.add(form, BorderLayout.NORTH);

        for (Weapon w : WeaponCatalogue.all()) {
            weaponModel.addElement(w);
        }
        weaponList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane weaponScroll = new JScrollPane(weaponList);
        weaponScroll.setBorder(BorderFactory.createTitledBorder("Weapons (multi-select)"));
        editor.add(weaponScroll, BorderLayout.CENTER);
        add(editor, BorderLayout.CENTER);

        nameField.getDocument().addDocumentListener(new SimpleDocListener(this::applyEdits));
        roleBox.addActionListener(e -> applyEdits());
        armourBox.addActionListener(e -> applyEdits());
        weaponList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                applyEdits();
            }
        });

        // --- bottom: live summary ---
        summary.setFont(new Font("SansSerif", Font.BOLD, 13));
        summary.setBorder(BorderFactory.createEmptyBorder(6, 4, 2, 4));
        add(summary, BorderLayout.SOUTH);

        rebuildRosterModel();
        if (!rosterModel.isEmpty()) {
            rosterList.setSelectedIndex(0);
        }
        updateSummary();
    }

    public Gang gang() {
        return gang;
    }

    private void rebuildRosterModel() {
        rosterModel.clear();
        for (Fighter f : gang.roster()) {
            rosterModel.addElement(f);
        }
    }

    private void addFighter() {
        String id = "F-" + (gang.roster().size() + 1) + "-" + System.identityHashCode(new Object());
        Fighter f = new Fighter(id, "New Fighter", FighterType.GANGER,
                StatLine.of(4, 3, 3, 3, 3, 1, 3, 1, 7));
        gang.add(f);
        rebuildRosterModel();
        rosterList.setSelectedValue(f, true);
        updateSummary();
    }

    private void removeFighter() {
        if (current != null) {
            gang.remove(current);
            current = null;
            rebuildRosterModel();
            if (!rosterModel.isEmpty()) {
                rosterList.setSelectedIndex(0);
            } else {
                clearEditor();
            }
            updateSummary();
        }
    }

    private void loadSelected() {
        current = rosterList.getSelectedValue();
        if (current == null) {
            clearEditor();
            return;
        }
        suppressEvents = true;
        nameField.setText(current.name());
        roleBox.setSelectedItem(current.type());
        armourBox.setSelectedItem(current.armour());
        int[] indices = new int[current.weapons().size()];
        int n = 0;
        for (int i = 0; i < weaponModel.size(); i++) {
            if (current.weapons().contains(weaponModel.get(i))) {
                indices[n++] = i;
            }
        }
        int[] trimmed = new int[n];
        System.arraycopy(indices, 0, trimmed, 0, n);
        weaponList.setSelectedIndices(trimmed);
        suppressEvents = false;
    }

    private void clearEditor() {
        suppressEvents = true;
        nameField.setText("");
        weaponList.clearSelection();
        suppressEvents = false;
    }

    private void applyEdits() {
        if (suppressEvents || current == null) {
            return;
        }
        current.setName(nameField.getText());
        current.setType((FighterType) roleBox.getSelectedItem());
        current.setArmour((Armour) armourBox.getSelectedItem());
        current.clearWeapons();
        for (Weapon w : weaponList.getSelectedValuesList()) {
            current.addWeapon(w);
        }
        rosterList.repaint();
        updateSummary();
    }

    private void updateSummary() {
        RosterRules.Result result = RosterRules.validate(gang);
        String legal = result.isLegal()
                ? "Legal roster ✓"
                : "Illegal: " + String.join("; ", result.problems());
        summary.setText("Gang rating: " + gang.rating()
                + " | Fighters: " + gang.roster().size()
                + " | " + legal);
        summary.setForeground(result.isLegal() ? new Color(0x1E, 0x8E, 0x3E) : new Color(0xC0, 0x39, 0x2B));
    }

    /** Minimal DocumentListener that just fires a Runnable on any change. */
    private static final class SimpleDocListener implements javax.swing.event.DocumentListener {
        private final Runnable onChange;
        SimpleDocListener(Runnable onChange) { this.onChange = onChange; }
        public void insertUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
        public void removeUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
        public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
    }
}
