package com.whim.stars.ui;

import java.awt.BorderLayout;
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
import javax.swing.SpinnerNumberModel;

import com.whim.stars.model.Planet;
import com.whim.stars.model.Player;
import com.whim.stars.model.production.ProductionItem;
import com.whim.stars.model.ship.ShipDesign;

/**
 * The Production screen for one planet: view and edit its build queue. Add
 * factories/mines/defenses/scanner/ships or open-ended auto-build entries,
 * reorder, and remove. Edits the planet's live queue directly.
 */
public final class ProductionDialog extends JDialog {

    private final Planet planet;
    private final Player human;
    private final DefaultListModel<ProductionItem> model = new DefaultListModel<ProductionItem>();
    private final JList<ProductionItem> list = new JList<ProductionItem>(model);

    public ProductionDialog(Frame owner, Player human, Planet planet) {
        super(owner, "Production — " + planet.name(), true);
        this.human = human;
        this.planet = planet;
        setLayout(new BorderLayout(8, 8));

        reload();
        list.setCellRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> l, Object value,
                    int index, boolean sel, boolean focus) {
                super.getListCellRendererComponent(l, value, index, sel, focus);
                if (value instanceof ProductionItem) {
                    setText(((ProductionItem) value).label());
                }
                return this;
            }
        });
        list.setPreferredSize(new Dimension(260, 300));
        add(new JScrollPane(list), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.EAST);
        add(buildSouth(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    private void reload() {
        model.clear();
        for (ProductionItem item : planet.productionQueue()) {
            model.addElement(item);
        }
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Add"));
        final JSpinner qty = new JSpinner(new SpinnerNumberModel(5, 1, 999, 1));

        panel.add(new JLabel("Quantity:"));
        panel.add(qty);
        panel.add(addButton("Factory", () -> ProductionItem.of(ProductionItem.Kind.FACTORY, (Integer) qty.getValue())));
        panel.add(addButton("Mine", () -> ProductionItem.of(ProductionItem.Kind.MINE, (Integer) qty.getValue())));
        panel.add(addButton("Defense", () -> ProductionItem.of(ProductionItem.Kind.DEFENSE, (Integer) qty.getValue())));
        panel.add(addButton("Planetary Scanner", () -> ProductionItem.of(ProductionItem.Kind.PLANETARY_SCANNER, 1)));
        panel.add(addButton("Auto Factories", () -> ProductionItem.auto(ProductionItem.Kind.AUTO_FACTORY)));
        panel.add(addButton("Auto Mines", () -> ProductionItem.auto(ProductionItem.Kind.AUTO_MINE)));

        JButton ship = new JButton("Ship…");
        ship.addActionListener(e -> addShip((Integer) qty.getValue()));
        panel.add(ship);
        return panel;
    }

    private interface ItemSupplier {
        ProductionItem get();
    }

    private JButton addButton(String label, final ItemSupplier supplier) {
        JButton b = new JButton(label);
        b.addActionListener(e -> {
            planet.productionQueue().add(supplier.get());
            reload();
        });
        return b;
    }

    private void addShip(int quantity) {
        List<ShipDesign> designs = human.designs();
        if (designs.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No ship designs yet — create one in Ship Designs.");
            return;
        }
        ShipDesign choice = (ShipDesign) JOptionPane.showInputDialog(this, "Build which design?",
                "Add Ship", JOptionPane.PLAIN_MESSAGE, null,
                designs.toArray(), designs.get(0));
        if (choice != null) {
            planet.productionQueue().add(ProductionItem.ship(choice, quantity));
            reload();
        }
    }

    private JPanel buildSouth() {
        JPanel south = new JPanel();
        JButton up = new JButton("Up");
        JButton down = new JButton("Down");
        JButton remove = new JButton("Remove");
        JButton close = new JButton("Close");
        up.addActionListener(e -> move(-1));
        down.addActionListener(e -> move(1));
        remove.addActionListener(e -> {
            int i = list.getSelectedIndex();
            if (i >= 0) {
                planet.productionQueue().remove(i);
                reload();
            }
        });
        close.addActionListener(e -> dispose());
        south.add(up);
        south.add(down);
        south.add(remove);
        south.add(close);
        return south;
    }

    private void move(int delta) {
        int i = list.getSelectedIndex();
        int j = i + delta;
        List<ProductionItem> q = planet.productionQueue();
        if (i < 0 || j < 0 || j >= q.size()) {
            return;
        }
        ProductionItem tmp = q.get(i);
        q.set(i, q.get(j));
        q.set(j, tmp);
        reload();
        list.setSelectedIndex(j);
    }
}
