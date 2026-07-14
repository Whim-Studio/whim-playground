package com.whim.stars.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.whim.stars.model.Cargo;
import com.whim.stars.model.Player;
import com.whim.stars.model.ship.Catalogue;
import com.whim.stars.model.ship.ComponentCategory;
import com.whim.stars.model.ship.HullType;
import com.whim.stars.model.ship.ShipDesign;

/**
 * The Ship Design screen: browse the player's designs, create a new one from a
 * catalogue hull, fill each typed slot from the compatible components, and see
 * the resulting mass / armor / shield / warp / firepower / cost live before
 * saving. Enforces the same slot-category rules the model does.
 */
public final class ShipDesignDialog extends JDialog {

    private final Player human;

    private final DefaultListModel<ShipDesign> designModel = new DefaultListModel<ShipDesign>();
    private final JList<ShipDesign> designList = new JList<ShipDesign>(designModel);

    private final JTextField nameField = new JTextField(18);
    private final JPanel slotsPanel = new JPanel();
    private final JTextArea stats = new JTextArea(10, 26);

    private ShipDesign editing;
    private boolean editingIsNew;
    private JComboBox<com.whim.stars.model.ship.Component>[] slotCombos;
    private JSpinner[] slotCounts;

    public ShipDesignDialog(Frame owner, Player human) {
        super(owner, "Ship Designs — " + human.name(), true);
        this.human = human;
        setLayout(new BorderLayout(8, 8));

        for (ShipDesign d : human.designs()) {
            designModel.addElement(d);
        }
        designList.setPreferredSize(new Dimension(180, 380));
        designList.addListSelectionListener(e -> {
            ShipDesign d = designList.getSelectedValue();
            if (d != null) {
                loadDesign(d, false);
            }
        });
        JPanel left = new JPanel(new BorderLayout(4, 4));
        left.add(new JScrollPane(designList), BorderLayout.CENTER);
        JButton newBtn = new JButton("New Design…");
        newBtn.addActionListener(e -> newDesign());
        left.add(newBtn, BorderLayout.SOUTH);
        add(left, BorderLayout.WEST);

        add(buildEditor(), BorderLayout.CENTER);

        stats.setEditable(false);
        stats.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        add(new JScrollPane(stats), BorderLayout.EAST);

        if (!designModel.isEmpty()) {
            designList.setSelectedIndex(0);
        }

        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel buildEditor() {
        JPanel editor = new JPanel(new BorderLayout(6, 6));
        editor.setBorder(BorderFactory.createTitledBorder("Design"));

        JPanel top = new JPanel();
        top.add(new JLabel("Name:"));
        top.add(nameField);
        editor.add(top, BorderLayout.NORTH);

        slotsPanel.setLayout(new GridLayout(0, 1, 4, 4));
        editor.add(new JScrollPane(slotsPanel), BorderLayout.CENTER);

        JPanel south = new JPanel();
        JButton save = new JButton("Save Design");
        save.addActionListener(e -> save());
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        south.add(save);
        south.add(close);
        editor.add(south, BorderLayout.SOUTH);
        return editor;
    }

    private void newDesign() {
        List<HullType> hulls = Catalogue.hulls();
        HullType hull = (HullType) JOptionPane.showInputDialog(this, "Choose a hull:",
                "New Design", JOptionPane.PLAIN_MESSAGE, null,
                hulls.toArray(), hulls.get(0));
        if (hull == null) {
            return;
        }
        loadDesign(new ShipDesign("New " + hull.name(), hull), true);
    }

    @SuppressWarnings("unchecked")
    private void loadDesign(ShipDesign design, boolean isNew) {
        this.editing = design;
        this.editingIsNew = isNew;
        nameField.setText(design.name());
        slotsPanel.removeAll();

        List<ShipDesign.Placement> placements = design.placements();
        slotCombos = new JComboBox[placements.size()];
        slotCounts = new JSpinner[placements.size()];

        for (int i = 0; i < placements.size(); i++) {
            ShipDesign.Placement p = placements.get(i);
            HullType.SlotDef slot = p.slot();

            JComboBox<com.whim.stars.model.ship.Component> combo =
                    new JComboBox<com.whim.stars.model.ship.Component>();
            combo.addItem(null); // "(empty)"
            for (com.whim.stars.model.ship.Component c : Catalogue.components()) {
                if (slot.accepts(c.category())) {
                    combo.addItem(c);
                }
            }
            combo.setSelectedItem(p.component());
            combo.setRenderer(new EmptyAwareRenderer());
            combo.addActionListener(e -> updateStats());

            JSpinner count = new JSpinner(new SpinnerNumberModel(
                    Math.max(1, p.count()), 1, slot.capacity(), 1));
            count.addChangeListener(e -> updateStats());

            slotCombos[i] = combo;
            slotCounts[i] = count;

            JPanel row = new JPanel(new BorderLayout(6, 0));
            row.add(new JLabel(slotLabel(slot)), BorderLayout.WEST);
            row.add(combo, BorderLayout.CENTER);
            row.add(count, BorderLayout.EAST);
            slotsPanel.add(row);
        }
        slotsPanel.revalidate();
        slotsPanel.repaint();
        updateStats();
    }

    private String slotLabel(HullType.SlotDef slot) {
        return slot.category().label() + " x" + slot.capacity() + ":";
    }

    /** Build a scratch design from the current selections without mutating the real one. */
    private ShipDesign preview() {
        ShipDesign d = new ShipDesign(nameField.getText(), editing.hull());
        for (int i = 0; i < slotCombos.length; i++) {
            com.whim.stars.model.ship.Component c =
                    (com.whim.stars.model.ship.Component) slotCombos[i].getSelectedItem();
            if (c != null) {
                d.place(i, c, (Integer) slotCounts[i].getValue());
            }
        }
        return d;
    }

    private void updateStats() {
        if (editing == null || slotCombos == null) {
            stats.setText("");
            return;
        }
        ShipDesign d = preview();
        Cargo mc = d.mineralCost();
        StringBuilder sb = new StringBuilder();
        sb.append("Hull      : ").append(d.hull().name()).append('\n');
        sb.append("Valid     : ").append(d.isValid() ? "yes" : "NO (needs an engine)").append('\n');
        sb.append("Mass      : ").append(d.totalMass()).append(" kT\n");
        sb.append("Armor     : ").append(d.totalArmor()).append('\n');
        sb.append("Shield    : ").append(d.totalShield()).append('\n');
        sb.append("Max warp  : ").append(d.maxWarp()).append('\n');
        sb.append("Fuel cap  : ").append(d.fuelCapacity()).append('\n');
        sb.append("Cargo cap : ").append(d.cargoCapacity()).append('\n');
        sb.append("Scan range: ").append(d.scanRange()).append('\n');
        sb.append("Firepower : ").append(d.totalWeaponPower()).append('\n');
        sb.append("Colonizer : ").append(d.canColonize() ? "yes" : "no").append('\n');
        sb.append("Cost      : ").append(d.resourceCost()).append(" res\n");
        sb.append("           I=").append(mc.ironium()).append(" B=").append(mc.boranium())
                .append(" G=").append(mc.germanium()).append('\n');
        stats.setText(sb.toString());
        stats.setCaretPosition(0);
    }

    private void save() {
        if (editing == null) {
            return;
        }
        editing.setName(nameField.getText().trim().isEmpty() ? "Design" : nameField.getText().trim());
        for (int i = 0; i < slotCombos.length; i++) {
            com.whim.stars.model.ship.Component c =
                    (com.whim.stars.model.ship.Component) slotCombos[i].getSelectedItem();
            editing.place(i, c, c == null ? 1 : (Integer) slotCounts[i].getValue());
        }
        if (!editing.isValid()) {
            JOptionPane.showMessageDialog(this,
                    "This design has no engine and cannot be built (starbases excepted).",
                    "Invalid design", JOptionPane.WARNING_MESSAGE);
        }
        if (editingIsNew) {
            human.addDesign(editing);
            designModel.addElement(editing);
            editingIsNew = false;
            designList.setSelectedValue(editing, true);
        } else {
            designList.repaint();
        }
        updateStats();
    }

    /** Renders a null component entry as "(empty)". */
    private static final class EmptyAwareRenderer extends javax.swing.DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value == null) {
                setText("(empty)");
            } else if (value instanceof com.whim.stars.model.ship.Component) {
                setText(((com.whim.stars.model.ship.Component) value).name());
            }
            return this;
        }
    }
}
